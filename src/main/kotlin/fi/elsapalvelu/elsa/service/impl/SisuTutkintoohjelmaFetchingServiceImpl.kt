package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.SisuHyClientBuilder
import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaFetchingService
import fi.elsapalvelu.elsa.web.rest.errors.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.web.rest.errors.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.web.rest.errors.JSON_MAPPING_ERROR
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
        val request = Request.Builder().url(endpointUrl).build()
        try {
            log.info("Haetaan erikoisalojen Sisu tutkinto-ohjelma id:t rajapinnasta $endpointUrl")
            return sisuHyClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR $endpointUrl ${response.body}")
                    return null
                }
                response.body?.string().let {
                    objectMapper.readValue(it, Qualifications::class.java)
                }
            }

        } catch (e: IOException) {
            log.error("$JSON_FETCHING_ERROR $endpointUrl ${e.message}")
        } catch (e: JsonProcessingException) {
            log.error("$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message} ")
        } catch (e: JsonMappingException) {
            log.error("$JSON_MAPPING_ERROR: $endpointUrl ${e.message} ")
        }
        return null
    }
}

data class Qualifications(val entities: List<Entity>?)

data class Entity(val code: String?, val requirementCollections: List<Requirement>?)

data class Requirement(val degreeProgrammeGroupIds: List<String>?)
