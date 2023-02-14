package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException

private const val attainmentsEndpoint = "attainments"

@Service
class SisuTreOpintosuorituksetFetchingServiceImpl(
    @Qualifier("SisuTre") private val sisuTreClientBuilder: OkHttpClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
    private val yliopistoRepository: YliopistoRepository
) : OpintosuorituksetFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? {
        val endpointUrl =
            "${applicationProperties.getSecurity().getSisuTre().endpointUrl!!}/$attainmentsEndpoint"
        val postBody = "{\"id\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()
        try {
            return sisuTreClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body?.string()}")
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, AttainmentsResponse::class.java)
                        ?.let { response ->
                            OpintosuorituksetPersistenceDTO(
                                yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
                                items = response.attainments.map {
                                    OpintosuoritusDTO(
                                        suorituspaiva = it.attainmentDate?.tryParseToLocalDate(),
                                        opintopisteet = it.credits,
                                        nimi_fi = it.courseUnit?.name?.fi,
                                        nimi_sv = it.courseUnit?.name?.sv,
                                        kurssikoodi = it.courseUnit?.code,
                                        hyvaksytty = it.grade.passed,
                                        arvio_fi = it.grade.name?.fi,
                                        arvio_sv = it.grade.name?.sv,
                                        vanhenemispaiva = it.expiryDate?.tryParseToLocalDate(),
                                        yliopistoOpintooikeusId = it.studyRightId
                                    )
                                }
                            )
                        }
                }
            }
        } catch (e: JsonProcessingException) {
            log.error(
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}"
            )
        } catch (e: JsonMappingException) {
            log.error(
                "$JSON_MAPPING_ERROR: $endpointUrl ${e.message} "
            )
        } catch (e: IOException) {
            log.error(
                "$JSON_FETCHING_ERROR: $endpointUrl ${e.message}"
            )
        }
        return null
    }

    override fun shouldFetchOpintosuoritukset(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.TAMPEREEN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.TAMPEREEN_YLIOPISTO
    }
}

data class AttainmentsResponse(val attainments: List<Attainment>)

data class Attainment(
    val state: String?,
    val attainmentDate: String?,
    val credits: Double?,
    val courseUnitId: String?,
    val courseUnit: CourseUnit?,
    val grade: Grade,
    val expiryDate: String?,
    val studyRightId: String?
)

data class CourseUnit(
    val code: String?,
    val name: LocalizedString?
)

data class Grade(
    val name: LocalizedString?,
    val passed: Boolean
)
