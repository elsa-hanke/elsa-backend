package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KuntaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TyoskentelyjaksoServiceImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val kuntaRepository: KuntaRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper
) : TyoskentelyjaksoService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        userId: String
    ): TyoskentelyjaksoDTO? {
        log.debug("Request to save Tyoskentelyjakso : $tyoskentelyjaksoDTO")

        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
            if (tyoskentelyjaksoDTO.erikoistuvaLaakariId == null) {
                tyoskentelyjaksoDTO.erikoistuvaLaakariId = kirjautunutErikoistuvaLaakari.id
            }

            if (tyoskentelyjaksoDTO.erikoistuvaLaakariId == kirjautunutErikoistuvaLaakari.id) {
                var tyoskentelyjakso = tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO)

                // Tarkistetaan päättymispäivä suoritusarvioinneille
                if (tyoskentelyjakso.isSuoritusarvioinnitNotEmpty()) {
                    tyoskentelyjakso.suoritusarvioinnit.forEach {
                        if (it.tapahtumanAjankohta!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                            return null
                        }
                    }
                }
                // Tarkistetaan päättymispäivä keskeytyksille
                if (tyoskentelyjakso.keskeytykset.isNotEmpty()) {
                    tyoskentelyjakso.keskeytykset.forEach {
                        if (it.paattymispaiva!!.isAfter(tyoskentelyjakso.paattymispaiva)) {
                            return null
                        }
                    }
                }

                tyoskentelyjakso.tyoskentelypaikka!!.kunta = kuntaRepository
                    .findByIdOrNull(tyoskentelyjaksoDTO.tyoskentelypaikka!!.kuntaId)
                tyoskentelyjakso = tyoskentelyjaksoRepository.save(tyoskentelyjakso)
                return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
            }
        }

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<TyoskentelyjaksoDTO> {
        log.debug("Request to get list of Tyoskentelyjakso by user id : $userId")

        return tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
            .mapTo(mutableListOf(), tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): TyoskentelyjaksoDTO? {
        log.debug("Request to get Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (kirjautunutErikoistuvaLaakari == it) {
                        return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
                    }
                }
            }
        }

        return null
    }

    override fun delete(id: Long, userId: String) {
        log.debug("Request to delete Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {kirjautunutErikoistuvaLaakari ->
                    if (
                        kirjautunutErikoistuvaLaakari == it &&
                        !tyoskentelyjakso.isSuoritusarvioinnitNotEmpty()
                    ) {
                        tyoskentelyjaksoRepository.deleteById(id)
                    }
                }
            }
        }
    }
}
