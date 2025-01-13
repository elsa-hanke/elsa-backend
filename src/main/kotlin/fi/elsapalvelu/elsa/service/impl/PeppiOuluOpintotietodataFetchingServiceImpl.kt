package fi.elsapalvelu.elsa.service.impl

import com.apollographql.apollo3.exception.ApolloException
import fi.elsapalvelu.elsa.OpintotietodataPeppiOuluQuery
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_PEPPI_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_PEPPI_KOULUTUS
import fi.elsapalvelu.elsa.config.YEK_KOULUTETTAVA_PEPPI_VIRTAKOODI
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.PeppiOpintooikeudenTila
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PeppiOuluOpintotietodataFetchingServiceImpl(
    @Qualifier("PeppiOulu") private val peppiOuluClientBuilder: GraphQLClientBuilder,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        try {
            val response =
                peppiOuluClientBuilder.apolloClient().query(OpintotietodataPeppiOuluQuery(id = hetu))
                    .execute()

            return response.data?.private_person_by_personal_identity_code?.let {
                OpintotietodataDTO(
                    syntymaaika = it.dateOfBirth?.tryParseToLocalDate(),
                    opintooikeudet = it.studyRights?.filter { opintooikeus ->
                        (opintooikeus.phase1EducationClassificationUrn == ERIKOISTUVA_LAAKARI_PEPPI_KOULUTUS ||
                            opintooikeus.phase1EducationClassificationUrn == ERIKOISTUVA_HAMMASLAAKARI_PEPPI_KOULUTUS) ||
                            opintooikeus.virtaPatevyyskoodi == YEK_KOULUTETTAVA_PEPPI_VIRTAKOODI
                    }?.map { opintooikeus ->
                        val erikoisalaTunniste = opintooikeus.virtaPatevyyskoodi
                        OpintotietoOpintooikeusDataDTO(
                            id = opintooikeus.id,
                            tila = mapOpintooikeudenTila(opintooikeus.state),
                            opintooikeudenAlkamispaiva = opintooikeus.valid?.startDate?.tryParseToLocalDate(),
                            opintooikeudenPaattymispaiva = opintooikeus.valid?.endDate?.tryParseToLocalDate(),
                            asetus = convertPeppiAsetusString(opintooikeus.decreeOnUniversityDegrees?.shortName?.fi),
                            erikoisalaTunnisteList = if (erikoisalaTunniste != null) listOf(erikoisalaTunniste) else null,
                            opiskelijatunnus = it.studentNumber,
                            yliopisto = YliopistoEnum.OULUN_YLIOPISTO
                        )
                    })
            }
        } catch (exception: ApolloException) {
            log.error("Opinto-oikeustietoja ei saatu haettua Oulun Pepistä. Virhe: ${exception.message} ${ExceptionUtils.getStackTrace(exception)}")
            return null
        }
    }

    override fun shouldFetchOpintotietodata(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.OULUN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.OULUN_YLIOPISTO
    }

    private fun mapOpintooikeudenTila(tila: String?): OpintooikeudenTila? {
        return when (tila) {
            PeppiOpintooikeudenTila.COMPLETED.toString(),
            PeppiOpintooikeudenTila.ATTENDING.toString() -> OpintooikeudenTila.AKTIIVINEN
            PeppiOpintooikeudenTila.ABSENT.toString(),
            PeppiOpintooikeudenTila.ABSENT_EXPENDING.toString(),
            PeppiOpintooikeudenTila.ABSENT_NON_EXPENDING.toString(),
            PeppiOpintooikeudenTila.OTHER_NON_EXPENDING.toString() -> OpintooikeudenTila.PASSIIVINEN
            PeppiOpintooikeudenTila.GRADUATED.toString() -> OpintooikeudenTila.VALMISTUNUT
            PeppiOpintooikeudenTila.RESIGNED.toString() -> OpintooikeudenTila.PERUUTETTU
            else -> null
        }
    }

    private fun convertPeppiAsetusString(peppiAsetus: String?): String? {
        return if (peppiAsetus == "TA-2020") {
            "55/2020"
        } else {
            peppiAsetus
        }
    }
}
