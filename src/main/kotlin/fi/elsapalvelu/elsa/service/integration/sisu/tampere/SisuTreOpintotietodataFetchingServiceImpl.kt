package fi.elsapalvelu.elsa.service.integration.sisu.tampere

import fi.elsapalvelu.elsa.required

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.*
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila.Companion.fromSisuOpintooikeudenTila
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.security.currentUserIdLogField
import fi.elsapalvelu.elsa.service.integration.AbstractOpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertKey
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.constants.JSON_DATA_PROSESSING_ERROR
import fi.elsapalvelu.elsa.service.constants.JSON_FETCHING_ERROR
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.SisuOpintooikeudenTila
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class SisuTreOpintotietodataFetchingServiceImpl(
    @Qualifier("SisuTre") private val sisuTreClientBuilder: OkHttpClientBuilder,
    private val applicationProperties: ApplicationProperties,
    private val objectMapper: ObjectMapper,
    private val integrationAlertService: IntegrationAlertService,
    yliopistoRepository: YliopistoRepository
) : AbstractOpintotietodataFetchingService(yliopistoRepository, YliopistoEnum.TAMPEREEN_YLIOPISTO) {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointUrl = "${applicationProperties.getSecurity().getSisuTre().endpointUrl.required()}/study-rights"
        val postBody = "{\"id\": \"$hetu\"}"
        val request = Request.Builder().url(endpointUrl).post(postBody.toRequestBody()).build()
        try {
            return sisuTreClientBuilder.okHttpClient().newCall(request).execute().use { response ->
                handleResponse(response, endpointUrl)
            }
        } catch (e: JsonProcessingException) {
            log.error("$JSON_DATA_PROSESSING_ERROR: $endpointUrl ${e.message}", e)
            throw e
        } catch (e: IOException) {
            log.error("$JSON_FETCHING_ERROR: $endpointUrl ${e.message}", e)
            throw e
        }
    }

    private fun handleResponse(response: Response, endpointUrl: String): OpintotietodataDTO? {
        if (!response.isSuccessful) {
            log.error("$JSON_FETCHING_ERROR: $endpointUrl ${response.body?.string()}")
            if (response.code in HTTP_SERVER_ERROR_RANGE) {
                recordInternalServerErrorFailure(endpointUrl, response.code)
            } else {
                integrationAlertService.recordConnectivitySuccess(
                    IntegrationAlertKey.SISU_TRE_INTERNAL_SERVER_ERROR,
                    endpointUrl
                )
            }
            return null
        }
        integrationAlertService.recordConnectivitySuccess(
            IntegrationAlertKey.SISU_TRE_INTERNAL_SERVER_ERROR,
            endpointUrl
        )
        return response.body?.string().let { body ->
            objectMapper.readValue(body, StudyRightsResponse::class.java)?.let { studyRightResponse ->
                toOpintotietodataDTO(studyRightResponse)
            }
        }
    }

    private fun toOpintotietodataDTO(studyRightResponse: StudyRightsResponse): OpintotietodataDTO {
        val acceptedStudyRights = studyRightResponse.studyrights?.filter {
            it.phase1EducationClassificationUrn == ERIKOISTUVA_LAAKARI_SISU_KOULUTUS ||
                it.phase1EducationClassificationUrn == ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS ||
                it.specialisation == YEK_KOULUTETTAVA_SISU_TRE_KOULUTUS
        }
        if (studyRightResponse.studyrights?.isNotEmpty() == true && acceptedStudyRights.isNullOrEmpty()) {
            log.warn(
                "Tampereen Sisusta saatiin ${studyRightResponse.studyrights.size} opinto-oikeutta, " +
                    "mutta yksikään ei vastannut ELSA:n tukemia koulutuksia.${currentUserIdLogField()}"
            )
        }
        return OpintotietodataDTO(
            syntymaaika = studyRightResponse.dateOfBirth?.tryParseToLocalDate(),
            opintooikeudet = acceptedStudyRights?.map {
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

    private fun recordInternalServerErrorFailure(endpointUrl: String, statusCode: Int) {
        integrationAlertService.recordConnectivityFailure(
            IntegrationAlertKey.SISU_TRE_INTERNAL_SERVER_ERROR,
            endpointUrl,
            "Tampereen Sisu study-rights-rajapinta palauttaa palvelinvirheitä",
            "Tampereen Sisun study-rights-rajapinta on palauttanut palvelinvirheen " +
                "$CONNECTIVITY_FAILURE_COUNT kertaa peräkkäin. HTTP status: $statusCode. " +
                "Endpoint: $endpointUrl. Virhe ei liity yksittäiseen henkilötunnukseen."
        )
    }
}

private const val HTTP_SERVER_ERROR_START = 500
private const val HTTP_SERVER_ERROR_END = 599
private val HTTP_SERVER_ERROR_RANGE = HTTP_SERVER_ERROR_START..HTTP_SERVER_ERROR_END
private const val CONNECTIVITY_FAILURE_COUNT = IntegrationAlertService.CONNECTIVITY_FAILURE_ALERT_THRESHOLD

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
