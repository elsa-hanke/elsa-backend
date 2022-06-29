package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDateTime
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.PeppiTurkuClientBuilder
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

private const val studyAccomplishmentsEndpoint = "study_accomplishments"

@Service
class PeppiTurkuOpintosuorituksetFetchingServiceImpl(
    private val peppiTurkuClientBuilder: PeppiTurkuClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
    private val yliopistoRepository: YliopistoRepository
) : OpintosuorituksetFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetDTO? {
        val endpointUrl =
            "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/$studyAccomplishmentsEndpoint"
        val postBody = "{\"hetu\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()
        try {
            return peppiTurkuClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body}")
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, object : TypeReference<List<StudyAccomplishment>>() {})
                        ?.let { accomplishments ->
                            OpintosuorituksetDTO(
                                yliopisto = YliopistoEnum.TURUN_YLIOPISTO,
                                items = accomplishments.map {
                                    OpintosuoritusDTO(
                                        suorituspaiva = it.suoritusPvm?.tryParseToLocalDateTime()?.toLocalDate(),
                                        opintopisteet = it.opintopisteet,
                                        nimi_fi = it.nimi?.fi,
                                        nimi_sv = it.nimi?.sv,
                                        kurssikoodi = it.kurssiKoodi,
                                        hyvaksytty = it.hyvaksytty,
                                        arvio_fi = it.arvio?.fi,
                                        arvio_sv = it.arvio?.sv,
                                        yliopistoOpintooikeusId = it.studyEntitlementKey
                                    )
                                }
                            )
                        }
                }
            }
        } catch (e: IOException) {
            log.error(
                "$JSON_FETCHING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: JsonMappingException) {
            log.error(
                "$JSON_MAPPING_ERROR: $endpointUrl ${e.message} "
            )
        }
        return null
    }

    override fun shouldFetchOpintosuoritukset(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.TURUN_YLIOPISTO)?.haeOpintotietodata == true
    }
}

data class StudyAccomplishment(
    val studyEntitlementId: String?,
    val studyEntitlementKey: String?,
    val kurssiKoodi: String?,
    val suoritusPvm: String?,
    val opintopisteet: Double?,
    val nimi: LocalizedString?,
    val hyvaksytty: Boolean?,
    val arvio: LocalizedString?
)

data class LocalizedString(
    val fi: String?,
    val sv: String?
)
