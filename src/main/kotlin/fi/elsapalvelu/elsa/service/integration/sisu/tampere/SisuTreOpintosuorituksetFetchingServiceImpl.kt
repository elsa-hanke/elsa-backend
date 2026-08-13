package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import fi.elsapalvelu.elsa.required

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.security.currentUserIdLogField
import fi.elsapalvelu.elsa.service.integration.AbstractOpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.LocalizedString
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusDTO
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class SisuTreOpintosuorituksetFetchingServiceImpl(
    @Qualifier("SisuTre") private val sisuTreClientBuilder: OkHttpClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
    yliopistoRepository: YliopistoRepository
) : AbstractOpintosuorituksetFetchingService(yliopistoRepository, YliopistoEnum.TAMPEREEN_YLIOPISTO) {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? {
        val endpointUrl =
            "${applicationProperties.getSecurity().getSisuTre().endpointUrl.required()}/attainments"
        val postBody = "{\"id\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()
        try {
            return sisuTreClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error(
                        "$JSON_FETCHING_ERROR: $endpointUrl${currentUserIdLogField()} " +
                            response.body?.string()
                    )
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, AttainmentsResponse::class.java)
                        ?.let { attainmentsResponse ->
                            OpintosuorituksetPersistenceDTO(
                                yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO,
                                items = attainmentsResponse.attainments.map {
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
                "$JSON_DATA_PROSESSING_ERROR: $endpointUrl${currentUserIdLogField()} ${e.message}",
                e
            )
            throw e
        } catch (e: IOException) {
            log.error("$JSON_FETCHING_ERROR: $endpointUrl${currentUserIdLogField()} ${e.message}", e)
            throw e
        }
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
