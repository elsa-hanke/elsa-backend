package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.SuoritemerkintaService
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.dto.UusiSuoritemerkintaDTO
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
    override fun create(
        uusiSuoritemerkintaDTO: UusiSuoritemerkintaDTO,
        userId: String
    ): List<SuoritemerkintaDTO>? {
        tyoskentelyjaksoRepository.findByIdOrNull(uusiSuoritemerkintaDTO.tyoskentelyjaksoId!!)
            ?.let { tyoskentelyjakso ->
                val kirjautunutErikoistuvaLaakari =
                    erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari != null
                    && kirjautunutErikoistuvaLaakari == tyoskentelyjakso.opintooikeus?.erikoistuvaLaakari
                ) {
                    uusiSuoritemerkintaDTO.suoritteet?.map {
                        suoritemerkintaMapper.toEntity(
                            SuoritemerkintaDTO(
                                tyoskentelyjaksoId = uusiSuoritemerkintaDTO.tyoskentelyjaksoId,
                                suorituspaiva = uusiSuoritemerkintaDTO.suorituspaiva,
                                lisatiedot = uusiSuoritemerkintaDTO.lisatiedot,
                                suoriteId = it.suoriteId,
                                arviointiasteikonTaso = it.arviointiasteikonTaso,
                                vaativuustaso = it.vaativuustaso,
                                arviointiasteikko = uusiSuoritemerkintaDTO.arviointiasteikko
                            )
                        )
                    }?.let { s ->
                        return suoritemerkintaRepository.saveAll(s)
                            .map { suoritemerkintaMapper.toDto(it) }
                    }
                }
            }
        return null
    }

    override fun save(suoritemerkintaDTO: SuoritemerkintaDTO, userId: String): SuoritemerkintaDTO? {
        tyoskentelyjaksoRepository.findByIdOrNull(suoritemerkintaDTO.tyoskentelyjaksoId!!)
            ?.let { tyoskentelyjakso ->
                val kirjautunutErikoistuvaLaakari =
                    erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari != null
                    && kirjautunutErikoistuvaLaakari == tyoskentelyjakso.opintooikeus?.erikoistuvaLaakari
                ) {
                    val suoritemerkinta = suoritemerkintaMapper.toEntity(suoritemerkintaDTO)
                    // Jos päivitetään olemassa olevaa, tarkistetaan että suoritemerkintä ei ole lukittu
                    if (suoritemerkinta.id != null) {
                        val suoritemerkintaOptional =
                            suoritemerkintaRepository.findOneById(suoritemerkinta.id!!)
                        if (suoritemerkintaOptional.isPresent && !suoritemerkintaOptional.get().lukittu) {
                            suoritemerkinta.arviointiasteikko =
                                suoritemerkintaOptional.get().arviointiasteikko
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
    override fun findAllByTyoskentelyjaksoOpintooikeusId(
        opintooikeusId: Long
    ): List<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId)
            .map(suoritemerkintaMapper::toDto)
    }

    override fun findForSeurantajakso(
        opintooikeusId: Long,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate
    ): List<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findForSeurantajakso(
            opintooikeusId,
            alkamispaiva,
            paattymispaiva
        )
            .map(suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): SuoritemerkintaDTO? {
        suoritemerkintaRepository.findByIdOrNull(id)?.let { suoritemerkinta ->
            suoritemerkinta.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari.let {
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
            suoritemerkinta.tyoskentelyjakso?.opintooikeus?.erikoistuvaLaakari.let {
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

    override fun existsBySuoriteId(suoriteId: Long): Boolean {
        return suoritemerkintaRepository.existsBySuoriteId(suoriteId)
    }
}
