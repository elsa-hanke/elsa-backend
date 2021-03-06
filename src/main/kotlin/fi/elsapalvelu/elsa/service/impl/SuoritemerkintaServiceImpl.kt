package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.SuoritemerkintaService
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritemerkintaMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SuoritemerkintaServiceImpl(
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val suoritemerkintaMapper: SuoritemerkintaMapper
) : SuoritemerkintaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(suoritemerkintaDTO: SuoritemerkintaDTO, userId: String): SuoritemerkintaDTO? {
        log.debug("Request to save Suoritemerkinta : $suoritemerkintaDTO")

        tyoskentelyjaksoRepository.findByIdOrNull(suoritemerkintaDTO.tyoskentelyjaksoId!!)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (kirjautunutErikoistuvaLaakari == it) {
                        val suoritemerkinta = suoritemerkintaMapper.toEntity(suoritemerkintaDTO)
                        // Jos päivitetään olemassa olevaa, tarkistetaan että suoritemerkintä ei ole lukittu
                        if (suoritemerkinta.id != null) {
                            val suoritemerkintaOptional = suoritemerkintaRepository.findOneById(suoritemerkinta.id!!)
                            if (suoritemerkintaOptional.isPresent && !suoritemerkintaOptional.get().lukittu) {
                                return suoritemerkintaMapper.toDto(suoritemerkintaRepository.save(suoritemerkinta))
                            }
                        } else {
                            return suoritemerkintaMapper.toDto(suoritemerkintaRepository.save(suoritemerkinta))
                        }
                    }
                }
            }
        }
        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): MutableList<SuoritemerkintaDTO> {
        log.debug("Request to get list of Suoritemerkinta by user id : $userId")

        return suoritemerkintaRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(userId)
            .mapTo(mutableListOf(), suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): SuoritemerkintaDTO? {
        log.debug("Request to get Suoritemerkinta : $id")

        suoritemerkintaRepository.findByIdOrNull(id)?.let { suoritemerkinta ->
            suoritemerkinta.tyoskentelyjakso?.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (kirjautunutErikoistuvaLaakari == it) {
                        return suoritemerkintaMapper.toDto(suoritemerkinta)
                    }
                }
            }
        }

        return null
    }

    override fun delete(id: Long, userId: String) {
        log.debug("Request to delete Suoritemerkinta : $id")

        suoritemerkintaRepository.findByIdOrNull(id)?.let { suoritemerkinta ->
            suoritemerkinta.tyoskentelyjakso?.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
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
