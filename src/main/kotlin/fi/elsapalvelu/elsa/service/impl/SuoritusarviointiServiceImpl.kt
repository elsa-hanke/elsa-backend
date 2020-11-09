package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.SuoritusarviointiService
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional
class SuoritusarviointiServiceImpl(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper
) : SuoritusarviointiService {

    override fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO {
        var suoritusarviointi = suoritusarviointiMapper.toEntity(suoritusarviointiDTO)
        suoritusarviointi = suoritusarviointiRepository.save(suoritusarviointi)
        // TODO: lähetä sähköposti arvioinnin tekijälle
        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    override fun save(
        suoritusarviointiDTO: SuoritusarviointiDTO,
        userId: String
    ): SuoritusarviointiDTO {
        var suoritusarviointi = suoritusarviointiRepository
            .findOneById(suoritusarviointiDTO.id!!).get()

        suoritusarviointi.tyoskentelyjakso?.erikoistuvaLaakari.let {
            val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository
                .findOneByKayttajaUserId(userId)
            if (kirjautunutErikoistuvaLaakari.isPresent && kirjautunutErikoistuvaLaakari.get() == it) {
                suoritusarviointi.itsearviointiVaativuustaso = suoritusarviointiDTO.itsearviointiVaativuustaso
                suoritusarviointi.itsearviointiLuottamuksenTaso = suoritusarviointiDTO.itsearviointiLuottamuksenTaso
                suoritusarviointi.sanallinenItsearviointi = suoritusarviointiDTO.sanallinenItsearviointi
                suoritusarviointi.itsearviointiAika = LocalDate.now(ZoneId.systemDefault())
            }
        }

        suoritusarviointi.arvioinninAntaja.let {
            val kirjautunutKayttaja = kayttajaRepository
                .findOneByUserId(userId)
            if (kirjautunutKayttaja.isPresent && kirjautunutKayttaja.get() == it) {
                suoritusarviointi.vaativuustaso = suoritusarviointiDTO.vaativuustaso
                suoritusarviointi.luottamuksenTaso = suoritusarviointiDTO.luottamuksenTaso
                suoritusarviointi.sanallinenArviointi = suoritusarviointiDTO.sanallinenArviointi
                suoritusarviointi.arviointiAika = LocalDate.now(ZoneId.systemDefault())
            }
        }

        suoritusarviointi = suoritusarviointiRepository.save(suoritusarviointi)
        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAll(pageable)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariId(
        erikoistuvaLaakariId: Long,
        pageable: Pageable
    ): Page<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariId(erikoistuvaLaakariId, pageable)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String,
        pageable: Pageable
    ): Page<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(userId, pageable)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): MutableList<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(userId)
            .mapTo(mutableListOf(), suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findById(id)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<SuoritusarviointiDTO> {
        return suoritusarviointiRepository.findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id, userId)
            .map(suoritusarviointiMapper::toDto)
    }

    override fun delete(id: Long) {
        suoritusarviointiRepository.deleteById(id)
    }
}
