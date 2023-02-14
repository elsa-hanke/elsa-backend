package fi.elsapalvelu.elsa.service.impl

import com.apollographql.apollo3.exception.ApolloException
import fi.elsapalvelu.elsa.OpintosuorituksetSisuHyQuery
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.extensions.tryParseToLocalDate
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.GraphQLClientBuilder
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusOsakokonaisuusDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

private const val attainedState: String = "ATTAINED"

@Service
class SisuHyOpintosuorituksetFetchingServiceImpl(
    @Qualifier("SisuHy") private val sisuHyClientBuilder: GraphQLClientBuilder,
    private val yliopistoRepository: YliopistoRepository
) : OpintosuorituksetFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? {
        return try {
            val response =
                sisuHyClientBuilder.apolloClient().query(OpintosuorituksetSisuHyQuery(id = hetu))
                    .execute()

            return response.data?.private_person_by_personal_identity_code?.attainments?.filter {
                it.state == attainedState
            }?.let {
                OpintosuorituksetPersistenceDTO(
                    yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
                    items = it.map { a ->
                        OpintosuoritusDTO(
                            suorituspaiva = a.attainmentDate?.tryParseToLocalDate(),
                            opintopisteet = a.credits,
                            nimi_fi = a.name?.fi ?: a.courseUnit?.name?.fi ?: a.module?.name?.fi,
                            nimi_sv = a.name?.sv ?: a.courseUnit?.name?.sv ?: a.module?.name?.fi,
                            kurssikoodi = a.code ?: a.courseUnit?.code ?: a.module?.code,
                            hyvaksytty = a.grade?.passed,
                            arvio_fi = a.grade?.name?.fi,
                            arvio_sv = a.grade?.name?.sv,
                            vanhenemispaiva = a.expiryDate?.tryParseToLocalDate(),
                            yliopistoOpintooikeusId = a.studyRightId,
                            osakokonaisuudet = a.childAttainments?.filter { c -> c.state == attainedState }
                                ?.map { c -> mapOsakokonaisuus(c) }
                        )
                    }
                )
            }
        } catch (exception: ApolloException) {
            log.error("Opintosuoritustietoja ei saatu haettua HY:n Sisusta. Virhe: ${exception.message}")
            null
        }
    }

    private fun mapOsakokonaisuus(osakokonaisuus: OpintosuorituksetSisuHyQuery.ChildAttainment): OpintosuoritusOsakokonaisuusDTO {
        return OpintosuoritusOsakokonaisuusDTO(
            suorituspaiva = osakokonaisuus.attainmentDate?.tryParseToLocalDate(),
            opintopisteet = osakokonaisuus.credits,
            nimi_fi = osakokonaisuus.name?.fi ?: osakokonaisuus.courseUnit?.name?.fi,
            nimi_sv = osakokonaisuus.name?.sv ?: osakokonaisuus.courseUnit?.name?.sv,
            kurssikoodi = osakokonaisuus.code ?: osakokonaisuus.courseUnit?.code,
            hyvaksytty = osakokonaisuus.grade?.passed,
            arvio_fi = osakokonaisuus.grade?.name?.fi,
            arvio_sv = osakokonaisuus.grade?.name?.sv,
            vanhenemispaiva = osakokonaisuus.expiryDate?.tryParseToLocalDate()
        )
    }

    override fun shouldFetchOpintosuoritukset(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.HELSINGIN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.HELSINGIN_YLIOPISTO
    }
}
