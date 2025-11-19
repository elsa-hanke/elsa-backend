package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.Erikoisala
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.io.File

@Service
class HelsinkiSiiloService(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(HelsinkiSiiloService::class.java)

    fun laheta(zipFilePath: String, erikoisala: Erikoisala) {
        val arkistointiProperties = applicationProperties.getArkistointi().getHki()
        val siiloKoodi = erikoisala.siiloKoodi ?: throw IllegalArgumentException("Erikoisalalta puuttuu siilo koodi")
        val url = "${arkistointiProperties.host}/unisign/elsa/archive/$siiloKoodi"

        val zipFile = File(zipFilePath)
        if (!zipFile.exists()) {
            throw IllegalArgumentException("Arkistointitiedostoa ei löydy: $zipFilePath")
        }

        val body = LinkedMultiValueMap<String, Any>().apply {
            add("file", FileSystemResource(zipFile))
        }

        val apiKey = arkistointiProperties.apiKey ?: throw IllegalStateException("Siilo api-avain puuttuu")
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
            set("X-Api-Key", apiKey)
        }

        val requestEntity = HttpEntity(body, headers)

        log.info("Lähetetään arkistointipaketti HY:lle osoitteeseen $url erikoisalalle ${erikoisala.nimi}")
        try {
            val response = restTemplate.postForEntity(url, requestEntity, String::class.java)
            log.info("HY arkistointivastaus: ${response.statusCode} ${response.body}")
        } finally {
            val deleted = zipFile.delete()
            if (!deleted) {
                log.warn("Tiedoston ${zipFile.name} poistaminen epäonnistui")
            }
        }
    }
}
