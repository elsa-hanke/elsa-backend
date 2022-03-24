package fi.elsapalvelu.elsa.service.impl

import com.apollographql.apollo3.exception.ApolloException
import fi.elsapalvelu.elsa.PrivatePersonByPersonalIdentityCodeQuery
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.SisuHyClientBuilder
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.SisuOpintooikeudenTila
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SisuHyServiceImpl(
    private val sisuHyClientBuilder: SisuHyClientBuilder,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        try {
            val response =
                sisuHyClientBuilder.apolloClient().query(PrivatePersonByPersonalIdentityCodeQuery(id = hetu))
                    .execute()

            return response.data?.private_person_by_personal_identity_code?.let {
                OpintotietodataDTO(
                    opiskelijatunnus = it.studentNumber,
                    syntymaaika = it.dateOfBirth,
                    yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
                    opintooikeudet = it.studyRights?.filter { opintooikeus ->
                        (opintooikeus.phase1EducationClassificationUrn == ERIKOISTUVA_LAAKARI_SISU_KOULUTUS ||
                            opintooikeus.phase1EducationClassificationUrn == ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS)
                    }?.map { opintooikeus ->
                        OpintotietoOpintooikeusDataDTO(
                            id = opintooikeus.id,
                            tila = mapOpintooikeudenTila(opintooikeus.state),
                            opintooikeudenAlkamispaiva = opintooikeus.valid?.startDate?.let { alkamispaiva ->
                                LocalDate.parse(
                                    alkamispaiva
                                )
                            },
                            opintooikeudenPaattymispaiva = opintooikeus.valid?.endDate?.let { paattymispaiva ->
                                LocalDate.parse(
                                    paattymispaiva
                                )
                            },
                            asetus = opintooikeus.decreeOnUniversityDegrees?.shortName?.fi,
                            erikoisalaTunniste = opintooikeus.acceptedSelectionPath?.educationPhase1GroupId
                        )
                    })
            }
        } catch (exception: ApolloException) {
            log.error("Opinto-oikeustietoja ei saatu haettua HY:n Sisusta. Virhe: ${exception.message}")
            return null
        }
    }

    override fun shouldFetchOpintotietodata(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.HELSINGIN_YLIOPISTO.toString())?.haeOpintotietodata == true
    }

    fun mapOpintooikeudenTila(sisuOpintooikeudenTila: String?): OpintooikeudenTila? {
        return when (sisuOpintooikeudenTila) {
            SisuOpintooikeudenTila.ACTIVE.toString() -> OpintooikeudenTila.AKTIIVINEN
            SisuOpintooikeudenTila.ACTIVE_NONATTENDING.toString() -> OpintooikeudenTila.AKTIIVINEN_EI_LASNA
            SisuOpintooikeudenTila.NOT_STARTED.toString(),
            SisuOpintooikeudenTila.PASSIVE.toString() -> OpintooikeudenTila.PASSIIVINEN
            SisuOpintooikeudenTila.GRADUATED.toString() -> OpintooikeudenTila.VALMISTUNUT
            SisuOpintooikeudenTila.RESCINDED.toString(),
            SisuOpintooikeudenTila.CANCELLED_BY_ADMINISTRATION.toString() -> OpintooikeudenTila.PERUUTETTU
            SisuOpintooikeudenTila.EXPIRED.toString() -> OpintooikeudenTila.VANHENTUNUT
            else -> null
        }
    }
}

