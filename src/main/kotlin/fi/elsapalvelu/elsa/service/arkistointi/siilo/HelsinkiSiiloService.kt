package fi.elsapalvelu.elsa.service.arkistointi.siilo

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiException
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
            "Arkistointitiedostoa ei löydy"
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

        log.info("Lähetetään arkistointipaketti HY Siiloon")
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw SynchronousArkistointiException(response.code)
                }
                log.info("HY Siilo arkistointi onnistui [httpStatus={}]", response.code)
            }
        } finally {
            val deleted = zipFile.delete()
            if (!deleted) {
                log.warn("Siilon väliaikaisen arkistointipaketin poistaminen epäonnistui")
            }
        }
    }
}
