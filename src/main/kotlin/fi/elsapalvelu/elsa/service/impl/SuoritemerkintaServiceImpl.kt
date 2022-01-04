package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.SuoritemerkintaService
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritemerkintaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class SuoritemerkintaServiceImpl(
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val suoritemerkintaMapper: SuoritemerkintaMapper
) : SuoritemerkintaService {

    override fun save(suoritemerkintaDTO: SuoritemerkintaDTO, userId: String): SuoritemerkintaDTO? {
        tyoskentelyjaksoRepository.findByIdOrNull(suoritemerkintaDTO.tyoskentelyjaksoId!!)
            ?.let { tyoskentelyjakso ->
                val kirjautunutErikoistuvaLaakari =
                    erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari != null
                    && kirjautunutErikoistuvaLaakari == tyoskentelyjakso.erikoistuvaLaakari
                ) {
                    val suoritemerkinta = suoritemerkintaMapper.toEntity(suoritemerkintaDTO)
                    // Jos päivitetään olemassa olevaa, tarkistetaan että suoritemerkintä ei ole lukittu
                    if (suoritemerkinta.id != null) {
                        val suoritemerkintaOptional =
                            suoritemerkintaRepository.findOneById(suoritemerkinta.id!!)
                        if (suoritemerkintaOptional.isPresent && !suoritemerkintaOptional.get().lukittu) {
                            suoritemerkinta.arviointiasteikko = suoritemerkintaOptional.get().arviointiasteikko
                            return suoritemerkintaMapper.toDto(
                                suoritemerkintaRepository.save(
                                    suoritemerkinta
                                )
                            )
                        }
                    } else {
                        return suoritemerkintaMapper.toDto(
                            suoritemerkintaRepository.save(
                                suoritemerkinta
                            )
                        )
                    }
                }
            }
        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): List<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
            userId
        )
            .map(suoritemerkintaMapper::toDto)
    }

    override fun findForSeurantajakso(
        userId: String,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findForSeurantajakso(userId, alkamispaiva, paattymispaiva)
            .map(suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): SuoritemerkintaDTO? {
        suoritemerkintaRepository.findByIdOrNull(id)?.let { suoritemerkinta ->
            suoritemerkinta.tyoskentelyjakso?.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                    ?.let { kirjautunutErikoistuvaLaakari ->
                        if (kirjautunutErikoistuvaLaakari == it) {
                            return suoritemerkintaMapper.toDto(suoritemerkinta)
                        }
                    }
            }
        }

        return null
    }

    override fun delete(id: Long, userId: String) {
        suoritemerkintaRepository.findByIdOrNull(id)?.let { suoritemerkinta ->
            suoritemerkinta.tyoskentelyjakso?.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                    ?.let { kirjautunutErikoistuvaLaakari ->
                        if (kirjautunutErikoistuvaLaakari == it &&
                            !suoritemerkinta.lukittu
                        ) {
                            suoritemerkintaRepository.deleteById(id)
                        }
                    }
            }
        }
    }
}
