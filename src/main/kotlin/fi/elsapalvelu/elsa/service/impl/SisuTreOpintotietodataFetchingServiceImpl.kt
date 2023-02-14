package fi.elsapalvelu.elsa.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila.Companion.fromSisuOpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_MAPPING_ERROR
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.SisuOpintooikeudenTila
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException

private const val studyRightsEndpoint = "study-rights"

@Service
class SisuTreOpintotietodataFetchingServiceImpl(
    @Qualifier("SisuTre") private val sisuTreClientBuilder: OkHttpClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointUrl = "${applicationProperties.getSecurity().getSisuTre().endpointUrl!!}/$studyRightsEndpoint"
        val postBody = "{\"id\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()
        try {
            return sisuTreClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body?.string()}")
                    return null
                }
                response.body?.string().let { body ->
                    objectMapper.readValue(body, StudyRightsResponse::class.java)?.let { studyRightResponse ->
                        OpintotietodataDTO(
                            syntymaaika = studyRightResponse.dateOfBirth?.tryParseToLocalDate(),
                            opintooikeudet = studyRightResponse.studyrights?.filter {
                                it.phase1EducationClassificationUrn == ERIKOISTUVA_LAAKARI_SISU_KOULUTUS ||
                                    it.phase1EducationClassificationUrn == ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS
                            }?.map {
                                OpintotietoOpintooikeusDataDTO(
                                    id = it.id,
                                    opiskelijatunnus = studyRightResponse.studentNumber,
                                    opintooikeudenAlkamispaiva = it.valid?.startDate?.tryParseToLocalDate(),
                                    opintooikeudenPaattymispaiva = it.valid?.endDate?.tryParseToLocalDate(),
                                    erikoisalaTunnisteList = if (it.specialisation != null) listOf(it.specialisation) else null,
                                    asetus = it.decreeOnUniversityDegrees?.shortName?.fi,
                                    tila = fromSisuOpintooikeudenTila(it.state),
                                    yliopisto = YliopistoEnum.TAMPEREEN_YLIOPISTO
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

    override fun shouldFetchOpintotietodata(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.TAMPEREEN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.TAMPEREEN_YLIOPISTO
    }
}

data class StudyRightsResponse(
    val studentNumber: String?,
    val dateOfBirth: String?,
    val studyrights: List<StudyRight>?
)

data class StudyRight(
    val id: String?,
    val valid: Valid?,
    val state: SisuOpintooikeudenTila?,
    val phase1EducationClassificationUrn: String?,
    val decreeOnUniversityDegrees: DecreeOnUniversityDegrees?,
    val specialisation: String?
)

data class Valid(
    val startDate: String?,
    val endDate: String?
)

data class DecreeOnUniversityDegrees(
    val shortName: ShortName?
)

data class ShortName(
    val fi: String?
)
