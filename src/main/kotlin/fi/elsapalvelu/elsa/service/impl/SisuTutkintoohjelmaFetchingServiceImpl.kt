package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.SisuHyClientBuilder
import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaFetchingService
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class SisuTutkintoohjelmaFetchingServiceImpl(
    private val sisuHyClientBuilder: SisuHyClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper
) : SisuTutkintoohjelmaFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetch(): Qualifications? {
        val endpointUrl = applicationProperties.getSecurity().getSisuHy().tutkintoohjelmaExportUrl!!
        val request =
            Request.Builder().url(endpointUrl).build()
        try {
            log.info("Haetaan erikoisalojen Sisu tutkinto-ohjelma id:t rajapinnasta $endpointUrl")
            return sisuHyClientBuilder.okHttpClient().newCall(request).execute().use {
                if (!it.isSuccessful) {
                    log.error("Erikoisalojen Sisu tutkinto-ohjelma id:tä ei saatu haettua rajapinnasta $endpointUrl. " +
                        "Response: $it")
                    return null
                }
                it.body!!.string().let { body ->
                    objectMapper.readValue(body, Qualifications::class.java)
                }
            }

        } catch (e: IOException) {
            log.error("Erikoisalojen Sisu tutkinto-ohjelma id:tä ei saatu haettua rajapinnasta $endpointUrl. " +
                "Virhe: ${e.message}")
        } catch (e: JsonProcessingException) {
            log.error("Rajapinnasta $endpointUrl haetun datan prosessointi epäonnistui. Virhe: ${e.message} " +
                "Virhe: ${e.message}")
        } catch (e: JsonMappingException) {
            log.error("Rajapinnasta $endpointUrl haetun datan mäppäys epäonnistui. Virhe: ${e.message} " +
                "Virhe: ${e.message}")
        }
        return null
    }
}

data class Qualifications(val entities: List<Entity>?)

data class Entity(val code: String?, val requirementCollections: List<Requirement>?)

data class Requirement(val degreeProgrammeGroupIds: List<String>?)
