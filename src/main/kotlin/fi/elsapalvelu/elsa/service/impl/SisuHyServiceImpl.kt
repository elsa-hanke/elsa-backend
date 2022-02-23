package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.PrivatePersonByPersonalIdentityCodeQuery
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.config.ERIKOISTUVA_LAAKARI_SISU_KOULUTUS
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.Yliopisto
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.ApolloClientService
import fi.elsapalvelu.elsa.service.OpintotietoDataFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintotietoDataDTO
import fi.elsapalvelu.elsa.service.dto.OpintotietoOpintooikeusDataDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.SisuOpintooikeudenTila
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SisuHyServiceImpl(
    private val apolloClientService: ApolloClientService,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietoDataFetchingService {

    override suspend fun fetchOpintotietoData(hetu: String): OpintotietoDataDTO? {
        val response =
            apolloClientService.getSisuHyApolloClient().query(PrivatePersonByPersonalIdentityCodeQuery(id = hetu))
                .execute()

        return response.data?.private_person_by_personal_identity_code?.let {
            OpintotietoDataDTO(
                opiskelijatunnus = it.studentNumber,
                syntymaaika = it.dateOfBirth,
                yliopisto = Yliopisto.HELSINGIN_YLIOPISTO,
                opintooikeudet = it.studyRights?.filter { opintooikeus ->
                    (opintooikeus.decreeOnUniversityDegrees?.shortName?.fi == ERIKOISTUVA_LAAKARI_SISU_KOULUTUS ||
                        opintooikeus.decreeOnUniversityDegrees?.shortName?.fi == ERIKOISTUVA_HAMMASLAAKARI_SISU_KOULUTUS)
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
                        tutkintoohjelmaId = opintooikeus.acceptedSelectionPath?.educationPhase1GroupId
                    )
                })
        }
    }

    override fun shouldFetchOpintotietoData(): Boolean {
        return yliopistoRepository.findOneByNimi(Yliopisto.HELSINGIN_YLIOPISTO.toString())?.haeOpintotietodata == true
    }

    fun mapOpintooikeudenTila(sisuOpintooikeudenTila: String?): OpintooikeudenTila? {
        return when (sisuOpintooikeudenTila) {
            SisuOpintooikeudenTila.ACTIVE.toString() -> OpintooikeudenTila.AKTIIVINEN
            SisuOpintooikeudenTila.PASSIVE.toString() -> OpintooikeudenTila.PASSIIVINEN
            SisuOpintooikeudenTila.GRADUATED.toString() -> OpintooikeudenTila.VALMISTUNUT
            else -> null
        }
    }
}


