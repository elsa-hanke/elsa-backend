package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.OpintotietodataSisuHyQuery
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.config.YEK_KOULUTETTAVA_SISU_HY_KOULUTUS
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila.Companion.fromSisuOpintooikeudenTilaStr
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class SisuHyOpintotietodataFetchingServiceImpl(
    @Qualifier("SisuHy") private val sisuHyClientBuilder: GraphQLClientBuilder,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val response = sisuHyClientBuilder.apolloClient()
            .query(OpintotietodataSisuHyQuery(id = hetu))
            .execute()

        // In Apollo 4, network errors are stored in response.exception instead of being thrown.
        response.exception?.let { ex ->
            log.error("Opinto-oikeustietoja ei saatu haettua HY:n Sisusta. Virhe: ${ex.message}", ex)
            throw ex
        }

        if (response.hasErrors()) {
            val errMsg = response.errors!!.joinToString { it.message }
            log.error("Opinto-oikeustietoja ei saatu haettua HY:n Sisusta. GraphQL-virheet: $errMsg")
            return null
        }

        return response.data?.private_person_by_personal_identity_code?.let {
            OpintotietodataDTO(
                syntymaaika = it.dateOfBirth?.tryParseToLocalDate(),
                opintooikeudet = it.studyRights?.filter { opintooikeus ->
                    (opintooikeus.phase1EducationClassificationUrn == ERIKOISTUVA_LAAKARI_SISU_KOULUTUS ||
                        opintooikeus.phase1EducationClassificationUrn == ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS ||
                        opintooikeus.acceptedSelectionPath?.educationPhase1GroupId == YEK_KOULUTETTAVA_SISU_HY_KOULUTUS
                        )
                }?.map { opintooikeus ->
                    val erikoisalaTunniste = opintooikeus.acceptedSelectionPath?.educationPhase1GroupId
                    OpintotietoOpintooikeusDataDTO(
                        id = opintooikeus.id,
                        tila = fromSisuOpintooikeudenTilaStr(opintooikeus.state),
                        opintooikeudenAlkamispaiva = opintooikeus.valid?.startDate?.tryParseToLocalDate(),
                        opintooikeudenPaattymispaiva = opintooikeus.valid?.endDate?.tryParseToLocalDate(),
                        asetus = opintooikeus.decreeOnUniversityDegrees?.shortName?.fi,
                        erikoisalaTunnisteList = if (erikoisalaTunniste != null) listOf(erikoisalaTunniste) else null,
                        opiskelijatunnus = it.studentNumber,
                        yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO
                    )
                })
        }
    }

    override fun shouldFetchOpintotietodata(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.HELSINGIN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.HELSINGIN_YLIOPISTO
    }
}
