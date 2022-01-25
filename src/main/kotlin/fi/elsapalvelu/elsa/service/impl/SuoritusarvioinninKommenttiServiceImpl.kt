package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.SuoritusarvioinninKommenttiRepository
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.SuoritusarvioinninKommenttiService
import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninKommenttiDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.SuoritusarvioinninKommenttiMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class SuoritusarvioinninKommenttiServiceImpl(
    private val suoritusarvioinninKommenttiRepository: SuoritusarvioinninKommenttiRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarvioinninKommenttiMapper: SuoritusarvioinninKommenttiMapper,
    private val kayttajaMapper: KayttajaMapper,
    private val mailService: MailService
) : SuoritusarvioinninKommenttiService {

    override fun save(
        suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO,
        userId: String
    ): SuoritusarvioinninKommenttiDTO {
        val kayttaja = kayttajaRepository.findOneByUserId(userId).get()
        val kayttajaDTO = kayttajaMapper.toDto(kayttaja)

        // Tarkisteaan, että muokkaaja on sama kuin kommentin tekijä
        if (suoritusarvioinninKommenttiDTO.kommentoija != null &&
            suoritusarvioinninKommenttiDTO.kommentoija != kayttajaDTO
        ) {
            throw IllegalArgumentException("Kommenttia voi muokata vain kommentin tekijä.")
        }

        var suoritusarvioinninKommentti =
            suoritusarvioinninKommenttiMapper.toEntity(suoritusarvioinninKommenttiDTO)

        // Asetetaan kommentoija uuteen kommenttiin
        if (suoritusarvioinninKommentti.kommentoija == null) {
            suoritusarvioinninKommentti.kommentoija = kayttaja
        }

        val suoritusarviointi = suoritusarviointiRepository
            .findOneById(suoritusarvioinninKommentti.suoritusarviointi?.id!!).get()
        if (kayttaja == suoritusarviointi.arvioinninAntaja ||
            kayttaja == suoritusarviointi.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari?.kayttaja
        ) {
            suoritusarvioinninKommentti =
                suoritusarvioinninKommenttiRepository.save(suoritusarvioinninKommentti)
            val user =
                if (kayttaja == suoritusarviointi.arvioinninAntaja) kayttajaRepository.findById(
                    suoritusarviointi.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id!!
                ).get().user!!
                else kayttajaRepository.findById(suoritusarviointi.arvioinninAntaja?.id!!)
                    .get().user!!
            mailService.sendEmailFromTemplate(
                user,
                "suoritusarvioinninKommenttiEmail.html",
                "email.suoritusarvioinninkommentti.title",
                properties = mapOf(Pair(MailProperty.ID, suoritusarviointi.id!!.toString()))
            )
            return suoritusarvioinninKommenttiMapper.toDto(suoritusarvioinninKommentti)
        } else {
            throw IllegalArgumentException(
                "Kommentin lisääjän täytyy olla joko arviointipyynnön tehnyt tai arvioinnin antaja."
            )
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<SuoritusarvioinninKommenttiDTO> {
        return suoritusarvioinninKommenttiRepository.findAll()
            .map(suoritusarvioinninKommenttiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoritusarvioinninKommenttiDTO> {
        return suoritusarvioinninKommenttiRepository.findById(id)
            .map(suoritusarvioinninKommenttiMapper::toDto)
    }

    override fun delete(id: Long) {
        suoritusarvioinninKommenttiRepository.deleteById(id)
    }
}
