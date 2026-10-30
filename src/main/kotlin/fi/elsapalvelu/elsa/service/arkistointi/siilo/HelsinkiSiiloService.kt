package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit

@Service
class HelsinkiSiiloService(
    private val applicationProperties: ApplicationProperties
) {
    private val log = LoggerFactory.getLogger(HelsinkiSiiloService::class.java)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Same Siilo multipart protocol; the deadline and safe failures apply only to background delivery. */
    fun lahetaAsync(zipFilePath: String, caseType: CaseType) {
        val properties = applicationProperties.getArkistointi().getHki()
        val code = properties.metadata?.getCaseMetadata(caseType)?.siiloKoodi.orEmpty()
        val host = properties.host.orEmpty()
        val key = properties.apiKey.orEmpty()
        val timeout = properties.asyncTransferTimeout
        requireAsyncConfiguration(code.isNotBlank() && code.all { it.isLetterOrDigit() || it in "-_" })
        val endpoint = host.toHttpUrlOrNull()
        requireAsyncConfiguration(endpoint != null && (endpoint.isHttps ||
            (properties.asyncAllowLoopbackHttpForTests && endpoint.host in setOf("127.0.0.1", "::1"))))
        requireAsyncConfiguration(key.isNotBlank() && key.none { it.isISOControl() })
        requireAsyncConfiguration(!timeout.isNegative && !timeout.isZero && timeout.toMillis() in 1..Int.MAX_VALUE.toLong())
        val file = File(zipFilePath)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("application/zip".toMediaType())).build()
        val request = Request.Builder().url("${host.trimEnd('/')}/unisign/elsa/archive/$code")
            .addHeader("X-Api-Key", key).post(body).build()
        // OkHttp's call timeout cancels the actual exchange, including connect/upload/response wait.
        val client = okHttpClient.newBuilder().callTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            // Siilo may process the uploaded ZIP longer than the legacy 15-second read timeout.
            .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val retryable = response.code in setOf(408, 425, 429) || response.code in 500..599
                    throw SiiloTransferException("SIILO_HTTP_REJECTED", retryable, response.code)
                }
                // Do not log or deserialize the remote body: it can contain personal data.
            }
        } catch (exception: java.io.IOException) {
            // The server may have accepted the file before the connection failed.
            throw SiiloTransferException("SIILO_DELIVERY_UNCONFIRMED", true, category = siiloIoCategory(exception))
        }
    }

    private fun requireAsyncConfiguration(valid: Boolean) {
        if (!valid) throw SiiloTransferException("SIILO_CONFIGURATION_INVALID", false)
    }

    fun laheta(zipFilePath: String, caseType: CaseType) {
        val arkistointiProperties = applicationProperties.getArkistointi().getHki()
        val case = arkistointiProperties.metadata?.getCaseMetadata(caseType) ?: throw IllegalArgumentException(
            "Arkistointia ${caseType.value} ei ole määritelty tyypille $caseType"
        )
        val siiloKoodi = case.siiloKoodi ?: throw IllegalArgumentException("Siilo koodi puuttuu")
        val url = "${arkistointiProperties.host}/unisign/elsa/archive/$siiloKoodi"

        val zipFile = File(zipFilePath)
        require(zipFile.exists()) {
            "Arkistointitiedostoa ei löydy: $zipFilePath"
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                zipFile.name,
                zipFile.asRequestBody("application/zip".toMediaType())
            )
            .build()
        val apiKey = arkistointiProperties.apiKey ?: error("Siilo api-avain puuttuu")
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Api-Key", apiKey)
            .post(requestBody)
            .build()

        log.info("Lähetetään arkistointipaketti HY:lle osoitteeseen $url")
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    throw RuntimeException(
                        "HY arkistointi epäonnistui: HTTP ${response.code} ${response.message}, " +
                            "URL: $url. Palvelimen vastaus: ${responseBody?.take(500) ?: "(tyhjä)"}"
                    )
                }
                log.info("HY arkistointivastaus: ${response.code} $responseBody")
            }
        } finally {
            val deleted = zipFile.delete()
            if (!deleted) {
                log.warn("Tiedoston ${zipFile.name} poistaminen epäonnistui")
            }
        }
    }
}
