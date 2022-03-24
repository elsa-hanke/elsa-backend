package fi.elsapalvelu.elsa.service.impl

import com.apollographql.apollo3.exception.ApolloException
import fi.elsapalvelu.elsa.OpintosuorituksetQuery
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.SisuHyClientBuilder
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusOsakokonaisuusDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

private const val attainedState: String = "ATTAINED"

@Service
class SisuHyOpintosuorituksetFetchingServiceImpl(
    private val sisuHyClientBuilder: SisuHyClientBuilder,
    private val yliopistoRepository: YliopistoRepository
) : OpintosuorituksetFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetDTO? {
        return try {
            val response =
                sisuHyClientBuilder.apolloClient().query(OpintosuorituksetQuery(id = hetu))
                    .execute()

            return response.data?.private_person_by_personal_identity_code?.attainments?.filter {
                it.state == attainedState
            }?.let {
                OpintosuorituksetDTO(
                    yliopisto = YliopistoEnum.HELSINGIN_YLIOPISTO,
                    items = it.map { a ->
                        OpintosuoritusDTO(
                            suorituspaiva = a.attainmentDate.let { s -> LocalDate.parse(s) },
                            opintopisteet = a.credits,
                            nimi_fi = a.name?.fi ?: a.courseUnit?.name?.fi ?: a.module?.name?.fi,
                            nimi_sv = a.name?.sv ?: a.courseUnit?.name?.sv ?: a.module?.name?.fi,
                            kurssikoodi = a.code ?: a.courseUnit?.code ?: a.module?.code,
                            hyvaksytty = a.grade?.passed,
                            arvio_fi = a.grade?.name?.fi,
                            arvio_sv = a.grade?.name?.sv,
                            vanhenemispaiva = a.expiryDate?.let { e -> LocalDate.parse(e) },
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

    private fun mapOsakokonaisuus(osakokonaisuus: OpintosuorituksetQuery.ChildAttainment): OpintosuoritusOsakokonaisuusDTO {
        return OpintosuoritusOsakokonaisuusDTO(
            suorituspaiva = osakokonaisuus.attainmentDate.let { s -> LocalDate.parse(s) },
            opintopisteet = osakokonaisuus.credits,
            nimi_fi = osakokonaisuus.name?.fi ?: osakokonaisuus.courseUnit?.name?.fi,
            nimi_sv = osakokonaisuus.name?.sv ?: osakokonaisuus.courseUnit?.name?.sv,
            kurssikoodi = osakokonaisuus.code ?: osakokonaisuus.courseUnit?.code,
            hyvaksytty = osakokonaisuus.grade?.passed,
            arvio_fi = osakokonaisuus.grade?.name?.fi,
            arvio_sv = osakokonaisuus.grade?.name?.sv,
            vanhenemispaiva = osakokonaisuus.expiryDate?.let { e -> LocalDate.parse(e) }
        )
    }

    override fun shouldFetchOpintosuoritukset(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.HELSINGIN_YLIOPISTO.toString())?.haeOpintotietodata == true
    }
}
