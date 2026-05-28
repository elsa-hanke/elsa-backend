package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.OpintosuorituksetPeppiOuluQuery
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

@Service
class PeppiOuluOpintosuorituksetFetchingServiceImpl(
    @Qualifier("PeppiOulu") private val peppiOuluClientBuilder: GraphQLClientBuilder,
    private val yliopistoRepository: YliopistoRepository
) : OpintosuorituksetFetchingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? {
        val response = peppiOuluClientBuilder.apolloClient()
            .query(OpintosuorituksetPeppiOuluQuery(id = hetu))
            .execute()

        // In Apollo 4, network errors are stored in response.exception instead of being thrown.
        response.exception?.let { ex ->
            log.error("Opintosuoritustietoja ei saatu haettua Oulun Pepistä. Virhe: ${ex.message}", ex)
            throw ex
        }

        if (response.hasErrors()) {
            val errMsg = response.errors!!.joinToString { it.message }
            log.error("Opintosuoritustietoja ei saatu haettua Oulun Pepistä. GraphQL-virheet: $errMsg")
            return null
        }

        return response.data?.private_person_by_personal_identity_code?.attainments?.let {
            OpintosuorituksetPersistenceDTO(
                yliopisto = YliopistoEnum.OULUN_YLIOPISTO,
                items = it.map { a ->
                    OpintosuoritusDTO(
                        suorituspaiva = a.attainmentDate?.tryParseToLocalDate(),
                        opintopisteet = a.credits,
                        nimi_fi = a.courseUnit?.name?.fi,
                        nimi_sv = a.courseUnit?.name?.sv,
                        kurssikoodi = a.courseUnit?.code,
                        hyvaksytty = a.grade?.passed,
                        arvio_fi = a.grade?.name?.fi,
                        arvio_sv = a.grade?.name?.sv,
                        vanhenemispaiva = a.expiryDate?.tryParseToLocalDate(),
                        yliopistoOpintooikeusId = a.studyRightId,
                        osakokonaisuudet = a.childAttainments?.map { c -> mapOsakokonaisuus(c) }
                    )
                }
            )
        }
    }

    private fun mapOsakokonaisuus(osakokonaisuus: OpintosuorituksetPeppiOuluQuery.ChildAttainment): OpintosuoritusOsakokonaisuusDTO {
        return OpintosuoritusOsakokonaisuusDTO(
            suorituspaiva = osakokonaisuus.attainmentDate?.tryParseToLocalDate(),
            opintopisteet = osakokonaisuus.credits,
            nimi_fi = osakokonaisuus.courseUnit?.name?.fi,
            nimi_sv = osakokonaisuus.courseUnit?.name?.sv,
            kurssikoodi = osakokonaisuus.courseUnit?.code,
            hyvaksytty = osakokonaisuus.grade?.passed,
            arvio_fi = osakokonaisuus.grade?.name?.fi,
            arvio_sv = osakokonaisuus.grade?.name?.sv,
            vanhenemispaiva = osakokonaisuus.expiryDate?.tryParseToLocalDate(),
        )
    }

    override fun shouldFetchOpintosuoritukset(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.OULUN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.OULUN_YLIOPISTO
    }
}
