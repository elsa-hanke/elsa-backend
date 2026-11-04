package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.arkistointi.http.ArchiveHttpFailure
import fi.elsapalvelu.elsa.service.arkistointi.http.ArchiveHttpEndpointPolicy
import fi.elsapalvelu.elsa.service.arkistointi.http.ArchiveHttpRequestProgress
import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

/** No redirects, HTTP logging, or transparent retries of non-idempotent POST requests. */
class DynastyClient internal constructor(
    private val settings: DynastyProperties,
    private val mapper: ObjectMapper,
    private val transport: OkHttpClient?
) {
    constructor(settings: DynastyProperties, mapper: ObjectMapper) : this(settings, mapper, null)

    val inProgressGrace get() = settings.inProgressGrace
    private var token: String? = null
    private var expires = Instant.EPOCH

    private val httpClient by lazy {
        transport ?: OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(settings.callTimeout).readTimeout(settings.callTimeout)
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()
    }

    fun validateConfiguration() {
        val c = settings
        listOf(c.baseUrl, c.tokenUrl).forEach { value ->
            validateUrl(value)
        }
        if (listOf(c.clientId,c.clientSecret,c.organization,c.scope,c.caseSchemaId,c.documentSchemaId).any { it.isNullOrBlank() } ||
            c.callTimeout.isZero || c.callTimeout.isNegative || c.inProgressGrace < c.callTimeout.multipliedBy(2).plusMinutes(1)) {
            throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
        }
    }

    private fun validateUrl(value: String?) {
        val uri = try { java.net.URI(value.orEmpty()) } catch (_: Exception) { throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID) }
        val invalidAddress = !ArchiveHttpEndpointPolicy.isAllowed(uri.scheme, uri.host, settings.allowLoopbackHttpForTests)
        val invalidParts = uri.rawUserInfo != null || uri.rawFragment != null || uri.rawQuery != null
        if (invalidAddress || invalidParts) throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)
    }

    private fun requireSetting(value: String?): String =
        value?.takeIf { it.isNotBlank() } ?: throw DynastyFailure(DynastyFailureCategory.CONFIGURATION_INVALID)

    @Synchronized
    private fun accessToken(): String {
        val cachedToken = token
        if (cachedToken != null && expires.isAfter(Instant.now().plusSeconds(60))) return cachedToken
        val body = FormBody.Builder().add("grant_type", "client_credentials").add("client_id", requireSetting(settings.clientId))
            .add("client_secret", requireSetting(settings.clientSecret)).add("scope", settings.scope).build()
        val request = Request.Builder().url(requireSetting(settings.tokenUrl)).header("Organization", requireSetting(settings.organization)).post(body).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw DynastyFailure(
                    if (response.code >= 500 || response.code == 429) DynastyFailureCategory.AUTH_TEMPORARILY_REJECTED
                    else DynastyFailureCategory.AUTH_FAILED
                )
                val data = mapper.readTree(response.peekBody(16384).string())
                val value = data.path("access_token").takeIf { it.isTextual }?.textValue()?.takeIf { it.isNotBlank() }
                    ?: throw DynastyFailure(DynastyFailureCategory.AUTH_FAILED)
                val seconds = data.path("expires_in").takeIf { it.isIntegralNumber }?.longValue()
                    ?: throw DynastyFailure(DynastyFailureCategory.AUTH_FAILED)
                if (seconds <= 60 || seconds > 86400) throw DynastyFailure(DynastyFailureCategory.AUTH_FAILED)
                token = value
                expires = Instant.now().plusSeconds(seconds)
                return value
            }
        } catch (e: DynastyFailure) { throw e }
        catch (_: Exception) { throw DynastyFailure(DynastyFailureCategory.AUTH_UNAVAILABLE) }
    }

    @Synchronized
    private fun invalidate() { token = null; expires = Instant.EPOCH }

    fun createCase(tosClass: String, metadata: DynastyCaseRequest): DynastyCaseId =
        post(endpoint("api/v1/cases").addQueryParameter("tosClass", tosClass).build(), json(metadata), DynastyResponseKind.CaseId)
    fun createTask(caseId: DynastyCaseId, title: String): DynastyTaskId =
        post(endpoint("api/v1/cases/$caseId/tasks").build(), json(DynastyTaskRequest(title)), DynastyResponseKind.TaskId)
    fun createDocument(metadata: DynastyDocumentMetadata, options: DynastyDocumentOptions, document: LoadedArkistointiAsiakirja): DynastyDocumentId {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("MetaData", mapper.writeValueAsString(metadata)).addFormDataPart("CaseAttachmentOptions", mapper.writeValueAsString(options))
            .addFormDataPart("File", document.filename, document.sisalto.toRequestBody(document.contentType.toMediaType())).build()
        return post(endpoint("api/v1/documents").build(), body, DynastyResponseKind.DocumentId)
    }
    fun archiveCase(caseId: DynastyCaseId) { post(endpoint("api/v1/cases/$caseId/state/Archived").build(), ByteArray(0).toRequestBody(), DynastyResponseKind.NoContent) }
    private fun json(value: Any) = mapper.writeValueAsString(value).toRequestBody("application/json".toMediaType())

    private fun endpoint(path: String): HttpUrl.Builder =
        requireSetting(settings.baseUrl).trimEnd('/').toHttpUrl().newBuilder().addPathSegments(path)

    private fun <T> post(url: HttpUrl, body: RequestBody, responseKind: DynastyResponseKind<T>): T {
        repeat(2) { attempt ->
            val bearer = accessToken() // Token failure cannot have created the requested object.
            val request = Request.Builder().url(url)
                .header("Organization", requireSetting(settings.organization)).header("Authorization", "Bearer $bearer")
                .header("Accept", responseKind.accept).post(body).build()
            val progress = ArchiveHttpRequestProgress()
            val client = httpClient.newBuilder().eventListener(progress).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.code == 401) {
                        invalidate()
                        if (attempt == 0) return@use
                        throw DynastyFailure(DynastyFailureCategory.AUTH_FAILED)
                    }
                    if (!response.isSuccessful) {
                        // Even 503/429 can originate from a gateway after upstream processing.
                        if (response.code in 400..499 && response.code !in setOf(408,429)) throw DynastyFailure(DynastyFailureCategory.HTTP_REJECTED)
                        throw DynastyFailure(DynastyFailureCategory.DELIVERY_UNCONFIRMED)
                    }
                    return responseKind.read(response)
                }
            } catch (e: DynastyFailure) { throw e }
            catch (e: IOException) {
                val failure = progress.failure(e)
                val category = when (failure) {
                    ArchiveHttpFailure.CONNECTION_FAILED -> DynastyFailureCategory.CONNECTION_FAILED
                    ArchiveHttpFailure.TLS_FAILED -> DynastyFailureCategory.TLS_FAILED
                    ArchiveHttpFailure.DELIVERY_UNCONFIRMED -> DynastyFailureCategory.DELIVERY_UNCONFIRMED
                }
                throw DynastyFailure(category)
            }
            catch (_: Exception) { throw DynastyFailure(DynastyFailureCategory.DELIVERY_UNCONFIRMED) }
        }
        throw DynastyFailure(DynastyFailureCategory.AUTH_FAILED)
    }
}
