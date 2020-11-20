package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.SuoritemerkintaService
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritemerkintaMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class SuoritemerkintaServiceImpl(
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val suoritemerkintaMapper: SuoritemerkintaMapper
) : SuoritemerkintaService {

    override fun save(suoritemerkintaDTO: SuoritemerkintaDTO): SuoritemerkintaDTO {
        var suoritemerkinta = suoritemerkintaMapper.toEntity(suoritemerkintaDTO)
        suoritemerkinta = suoritemerkintaRepository.save(suoritemerkinta)
        return suoritemerkintaMapper.toDto(suoritemerkinta)
    }

    override fun save(suoritemerkintaDTO: SuoritemerkintaDTO, userId: String): SuoritemerkintaDTO {
        val tyoskentelyjaksoOpt = tyoskentelyjaksoRepository.findById(suoritemerkintaDTO.tyoskentelyjaksoId!!)
        if (tyoskentelyjaksoOpt.isPresent) {
            val tyoskentelyjakso = tyoskentelyjaksoOpt.get()
            tyoskentelyjakso.erikoistuvaLaakari.let {
                val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository
                    .findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari.isPresent &&
                    kirjautunutErikoistuvaLaakari.get() == it
                ) {
                    var suoritemerkinta = suoritemerkintaMapper.toEntity(suoritemerkintaDTO)
                    suoritemerkinta = suoritemerkintaRepository.save(suoritemerkinta)
                    return suoritemerkintaMapper.toDto(suoritemerkinta)
                }
            }
        }
        throw IllegalArgumentException("Suoritemerkinnän työskentelyjakso täytyy kuulua erikoistuvalle lääkärille.")
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findAll(pageable)
            .map(suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: String
    ): MutableList<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id)
            .mapTo(mutableListOf(), suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findById(id)
            .map(suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): Optional<SuoritemerkintaDTO> {
        val suoritemerkintaOpt = suoritemerkintaRepository.findOneById(id)
        if (suoritemerkintaOpt.isPresent) {
            val suoritemerkinta = suoritemerkintaOpt.get()
            suoritemerkinta.tyoskentelyjakso?.erikoistuvaLaakari.let {
                val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository
                    .findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari.isPresent &&
                    kirjautunutErikoistuvaLaakari.get() == it &&
                    !suoritemerkinta.lukittu
                ) {
                    return suoritemerkintaOpt.map(suoritemerkintaMapper::toDto)
                }
            }
        }

        return Optional.empty()
    }

    override fun delete(id: Long) {
        suoritemerkintaRepository.deleteById(id)
    }

    override fun delete(id: Long, userId: String) {
        val suoritemerkintaOpt = suoritemerkintaRepository.findOneById(id)
        if (suoritemerkintaOpt.isPresent) {
            val suoritemerkinta = suoritemerkintaOpt.get()
            suoritemerkinta.tyoskentelyjakso?.erikoistuvaLaakari.let {
                val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository
                    .findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari.isPresent &&
                    kirjautunutErikoistuvaLaakari.get() == it &&
                    !suoritemerkinta.lukittu
                ) {
                    suoritemerkintaRepository.deleteById(id)
                }
            }
        }
    }
}
