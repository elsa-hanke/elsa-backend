package fi.elsapalvelu.elsa.service.integration.sisu

import fi.elsapalvelu.elsa.required

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.integration.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertKey
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.hasSslCause
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class SisuTutkintoohjelmaFetchingServiceImpl(
    @Qualifier("SisuHy") private val sisuHyClientBuilder: GraphQLClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
    private val integrationAlertService: IntegrationAlertService
) : SisuTutkintoohjelmaFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetch(): Qualifications? {
        val endpointUrl = applicationProperties.getSecurity().getSisuHy().tutkintoohjelmaExportUrl.required()
        val request = Request.Builder().url(endpointUrl).build()
        try {
            log.info("Haetaan erikoisalojen Sisu tutkinto-ohjelma id:t rajapinnasta $endpointUrl")
            return sisuHyClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR $endpointUrl ${response.body?.string()}")
                    if (response.code in HTTP_CLIENT_ERROR_RANGE && response.code != HTTP_UNAUTHORIZED) {
                        publishFetchingFailureAlert(endpointUrl, "HTTP status: ${response.code}.")
                    }
                    return null
                }
                integrationAlertService.markSuccessful(IntegrationAlertKey.SISU_HY_AUTHENTICATION)
                response.body?.string().let {
                    objectMapper.readValue(it, Qualifications::class.java).also {
                        integrationAlertService.markSuccessful(
                            IntegrationAlertKey.SISU_HY_QUALIFICATION_EXPORT
                        )
                    }
                }
            }

        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}"
            )
            publishFetchingFailureAlert(endpointUrl, "Vastauksen JSON-käsittely epäonnistui.")
        } catch (e: IOException) {
            log.error(
                "$JSON_FETCHING_ERROR: $endpointUrl ${e.message}"
            )
            if (!e.hasSslCause()) {
                publishFetchingFailureAlert(endpointUrl, "Yhteyden muodostaminen epäonnistui.")
            }
        }
        return null
    }

    private fun publishFetchingFailureAlert(endpointUrl: String, reason: String) {
        integrationAlertService.publishOnceUntilSuccess(
            IntegrationAlertKey.SISU_HY_QUALIFICATION_EXPORT,
            "Helsingin Sisu tutkinto-ohjelmatuonti epäonnistui",
            "Helsingin Sisun HETU-riippumaton tutkinto-ohjelmatuonti epäonnistui. " +
                "$reason Endpoint: $endpointUrl."
        )
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        val HTTP_CLIENT_ERROR_RANGE = 400..499
    }
}

data class Qualifications(val entities: List<Entity>?)

data class Entity(val code: String?, val requirementCollections: List<Requirement>?)

data class Requirement(val degreeProgrammeGroupIds: List<String>?)
