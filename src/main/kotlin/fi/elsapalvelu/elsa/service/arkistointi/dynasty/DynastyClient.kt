package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.UnknownHostException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class DynastyFailure(val code: String, val retryable: Boolean = false, val definitelyNotProcessed: Boolean = false) :
    RuntimeException("Dynasty request failed [$code]")

/** No redirects, HTTP logging, or transparent retries of non-idempotent POST requests. */
@Component
class DynastyClient(private val properties: ApplicationProperties, private val mapper: ObjectMapper) {
    private val settings get() = properties.getArkistointi().getTurku().dynasty
    private var token: String? = null
    private var expires = Instant.EPOCH

    private fun client() = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .callTimeout(settings.callTimeout).readTimeout(settings.callTimeout)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()

    fun validateConfiguration() {
        val c = settings
        listOf(c.baseUrl, c.tokenUrl).forEach { value ->
            validateUrl(value)
        }
        if (listOf(c.clientId,c.clientSecret,c.organization,c.scope,c.caseSchemaId,c.documentSchemaId).any { it.isNullOrBlank() } ||
            c.callTimeout.isZero || c.callTimeout.isNegative || c.inProgressGrace < c.callTimeout.multipliedBy(2).plusMinutes(1)) {
            throw DynastyFailure("DYNASTY_CONFIGURATION_INVALID")
        }
    }

    private fun validateUrl(value: String?) {
        val uri = try { java.net.URI(value.orEmpty()) } catch (_: Exception) { throw DynastyFailure("DYNASTY_CONFIGURATION_INVALID") }
        val local = settings.allowLoopbackHttpForTests && uri.scheme == "http" && uri.host in setOf("127.0.0.1", "[::1]", "::1")
        val invalidAddress = (!local && uri.scheme != "https") || uri.host.isNullOrBlank()
        val invalidParts = uri.rawUserInfo != null || uri.rawFragment != null || uri.rawQuery != null
        if (invalidAddress || invalidParts) throw DynastyFailure("DYNASTY_CONFIGURATION_INVALID")
    }

    private fun requireSetting(value: String?): String =
        value?.takeIf { it.isNotBlank() } ?: throw DynastyFailure("DYNASTY_CONFIGURATION_INVALID")

    private fun readId(raw: String, guid: Boolean): String {
        if (raw.length > 1024) throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
        val id = raw.trim().removeSurrounding("\"")
        val valid = if (guid) runCatching { UUID.fromString(id).toString().equals(id, true) }.getOrDefault(false)
            else id.toLongOrNull()?.let { it > 0 } == true && id.all { it.isDigit() }
        if (!valid) throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
        return id
    }

    @Synchronized
    private fun accessToken(): String {
        val cachedToken = token
        if (cachedToken != null && expires.isAfter(Instant.now().plusSeconds(60))) return cachedToken
        val body = FormBody.Builder().add("grant_type", "client_credentials").add("client_id", requireSetting(settings.clientId))
            .add("client_secret", requireSetting(settings.clientSecret)).add("scope", settings.scope).build()
        val request = Request.Builder().url(requireSetting(settings.tokenUrl)).header("Organization", requireSetting(settings.organization)).post(body).build()
        try {
            client().newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw DynastyFailure("DYNASTY_AUTH_FAILED", response.code >= 500 || response.code == 429, true)
                val data = mapper.readTree(response.peekBody(16384).string())
                val value = data.path("access_token").takeIf { it.isTextual }?.textValue()?.takeIf { it.isNotBlank() }
                    ?: throw DynastyFailure("DYNASTY_AUTH_FAILED", false, true)
                val seconds = data.path("expires_in").takeIf { it.isIntegralNumber }?.longValue()
                    ?: throw DynastyFailure("DYNASTY_AUTH_FAILED", false, true)
                if (seconds <= 60 || seconds > 86400) throw DynastyFailure("DYNASTY_AUTH_FAILED", false, true)
                token = value
                expires = Instant.now().plusSeconds(seconds)
                return value
            }
        } catch (e: DynastyFailure) { throw e }
        catch (_: Exception) { throw DynastyFailure("DYNASTY_AUTH_UNAVAILABLE", true, true) }
    }

    @Synchronized
    private fun invalidate() { token = null; expires = Instant.EPOCH }

    fun createCase(tosClass: String, metadata: String): String = post("/api/v1/cases?tosClass=$tosClass", json(metadata), false)
    fun createTask(caseId: String, title: String): String = post("/api/v1/cases/$caseId/tasks", json(mapper.writeValueAsString(mapOf("title" to title))), true)
    fun createDocument(metadata: String, options: String, document: LoadedArkistointiAsiakirja): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("MetaData", metadata).addFormDataPart("CaseAttachmentOptions", options)
            .addFormDataPart("File", document.filename, document.sisalto.toRequestBody(document.contentType.toMediaType())).build()
        return post("/api/v1/documents", body, false)
    }
    fun archiveCase(caseId: String) { post("/api/v1/cases/$caseId/state/Archived", ByteArray(0).toRequestBody(), false, true) }
    private fun json(value: String) = value.toRequestBody("application/json".toMediaType())

    private fun post(path: String, body: RequestBody, guid: Boolean, empty: Boolean = false): String {
        repeat(2) { attempt ->
            val bearer = accessToken() // Token failure cannot have created the requested object.
            val request = Request.Builder().url(requireSetting(settings.baseUrl).trimEnd('/') + path)
                .header("Organization", requireSetting(settings.organization)).header("Authorization", "Bearer $bearer")
                .header("Accept", if (empty) "application/json" else "text/plain").post(body).build()
            try {
                client().newCall(request).execute().use { response ->
                    if (response.code == 401) {
                        invalidate()
                        if (attempt == 0) return@use
                        throw DynastyFailure("DYNASTY_AUTH_FAILED", false, true)
                    }
                    if (!response.isSuccessful) {
                        // Even 503/429 can originate from a gateway after upstream processing.
                        if (response.code in 400..499 && response.code !in setOf(408,429)) throw DynastyFailure("DYNASTY_HTTP_REJECTED", false, true)
                        throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED")
                    }
                    if (empty) return ""
                    return readId(response.peekBody(1025).string(),guid)
                }
            } catch (e: DynastyFailure) { throw e }
            catch (_: ConnectException) { throw DynastyFailure("DYNASTY_UNAVAILABLE", true, true) }
            catch (_: UnknownHostException) { throw DynastyFailure("DYNASTY_UNAVAILABLE", true, true) }
            catch (_: Exception) { throw DynastyFailure("DYNASTY_DELIVERY_UNCONFIRMED") }
        }
        throw DynastyFailure("DYNASTY_AUTH_FAILED", false, true)
    }
}
