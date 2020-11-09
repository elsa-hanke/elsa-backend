package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.SuoritusarvioinninKommenttiRepository
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.SuoritusarvioinninKommenttiService
import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninKommenttiDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.SuoritusarvioinninKommenttiMapper
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class SuoritusarvioinninKommenttiServiceImpl(
    private val suoritusarvioinninKommenttiRepository: SuoritusarvioinninKommenttiRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarvioinninKommenttiMapper: SuoritusarvioinninKommenttiMapper,
    private val kayttajaMapper: KayttajaMapper
) : SuoritusarvioinninKommenttiService {

    override fun save(suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO): SuoritusarvioinninKommenttiDTO {
        var suoritusarvioinninKommentti = suoritusarvioinninKommenttiMapper.toEntity(suoritusarvioinninKommenttiDTO)
        suoritusarvioinninKommentti = suoritusarvioinninKommenttiRepository.save(suoritusarvioinninKommentti)
        return suoritusarvioinninKommenttiMapper.toDto(suoritusarvioinninKommentti)
    }

    override fun save(
        suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO,
        userId: String
    ): SuoritusarvioinninKommenttiDTO {
        val kayttaja = kayttajaRepository.findOneByUserId(userId).get()
        suoritusarvioinninKommenttiDTO.kommentoija = kayttajaMapper.toDto(kayttaja)
        var suoritusarvioinninKommentti = suoritusarvioinninKommenttiMapper.toEntity(suoritusarvioinninKommenttiDTO)
        val suoritusarviointi = suoritusarviointiRepository
            .findOneById(suoritusarvioinninKommentti.suoritusarviointi?.id!!).get()
        if (kayttaja == suoritusarviointi.arvioinninAntaja ||
            kayttaja == suoritusarviointi.tyoskentelyjakso?.erikoistuvaLaakari?.kayttaja) {
            suoritusarvioinninKommentti = suoritusarvioinninKommenttiRepository.save(suoritusarvioinninKommentti)
            // TODO: lähetä sähköposti toiselle osapuolelle
            return suoritusarvioinninKommenttiMapper.toDto(suoritusarvioinninKommentti)
        } else {
            throw BadRequestAlertException(
                "Kommentin lisääjän täytyy olla joko arviointipyynnön tehnyt tai arvioinnin antaja.",
                "suoritusarvioinnin_kommentti", "dataillegal"
            )
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<SuoritusarvioinninKommenttiDTO> {
        return suoritusarvioinninKommenttiRepository.findAll()
            .mapTo(mutableListOf(), suoritusarvioinninKommenttiMapper::toDto)
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
