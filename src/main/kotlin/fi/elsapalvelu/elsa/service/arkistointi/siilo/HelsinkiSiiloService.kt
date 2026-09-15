package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

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

        logPayloadDetails(zipFile, url, siiloKoodi)

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

        log.info(
            "Lähetetään arkistointipaketti HY:lle osoitteeseen $url, " +
                "multipart-rungon koko: ${requestBody.contentLength()} tavua"
        )
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

    /**
     * Logs the exact payload we are about to send to the Helsinki Siilo endpoint: the ZIP
     * file's name/size and, if it can be read as a ZIP, the name and (uncompressed) size of
     * every entry inside it. This is useful for diagnosing cases where the receiving system
     * returns an error (e.g. HTTP 403) that may be related to an unexpectedly small or
     * otherwise malformed request body.
     */
    private fun logPayloadDetails(zipFile: File, url: String, siiloKoodi: String) {
        log.info(
            "HY arkistointipaketti valmis lähetettäväksi: siiloKoodi=$siiloKoodi, url=$url, " +
                "tiedosto=${zipFile.name}, koko=${zipFile.length()} tavua"
        )
        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries().toList()
                log.info("HY arkistointipaketin sisältö (${entries.size} tiedostoa):")
                entries.forEach { entry ->
                    log.info(
                        "  - ${entry.name}: pakattu=${entry.compressedSize} tavua, " +
                            "pakkaamaton=${entry.size} tavua"
                    )
                }
            }
        } catch (e: Exception) {
            log.warn("HY arkistointipaketin sisältöä ei voitu lukea ZIP-tiedostona: ${e.message}")
        }
    }
}

u
