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
import java.util.Optional

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

        tyoskentelyjaksoDTO.erikoistuvaLaakariId

        val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository
            .findOneByKayttajaUserId(userId)
        if (kirjautunutErikoistuvaLaakari.isPresent) {
            tyoskentelyjaksoDTO.erikoistuvaLaakariId = kirjautunutErikoistuvaLaakari.get().id
            var tyoskentelyjakso = tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO)
            tyoskentelyjakso.tyoskentelypaikka!!.kunta = kuntaRepository.findByIdOrNull(tyoskentelyjaksoDTO.tyoskentelypaikka!!.kuntaId)
            tyoskentelyjakso = tyoskentelyjaksoRepository.save(tyoskentelyjakso)
            return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        }

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<TyoskentelyjaksoDTO> {
        log.debug("Request to get list of Tyoskentelyjakso by user id : $userId")

        return tyoskentelyjaksoRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
            .mapTo(mutableListOf(), tyoskentelyjaksoMapper::toDto)
    }

    // TODO: Käytä user id rajapintaa
    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<TyoskentelyjaksoDTO> {
        log.debug("Request to get Tyoskentelyjakso : $id")

        return tyoskentelyjaksoRepository.findById(id)
            .map(tyoskentelyjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): TyoskentelyjaksoDTO? {
        log.debug("Request to get Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository
                    .findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari.isPresent &&
                    kirjautunutErikoistuvaLaakari.get() == it
                ) {
                    return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
                }
            }
        }

        return null
    }

    override fun delete(id: Long, userId: String) {
        log.debug("Request to delete Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.findByIdOrNull(id)?.let { tyoskentelyjakso ->
            tyoskentelyjakso.erikoistuvaLaakari.let {
                val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository
                    .findOneByKayttajaUserId(userId)
                if (kirjautunutErikoistuvaLaakari.isPresent &&
                    kirjautunutErikoistuvaLaakari.get() == it &&
                    tyoskentelyjakso.suoritusarvioinnit.isEmpty()
                ) {
                    tyoskentelyjaksoRepository.deleteById(id)
                }
            }
        }
    }
}
