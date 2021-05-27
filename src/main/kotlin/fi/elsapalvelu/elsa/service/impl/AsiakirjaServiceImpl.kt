package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.projection.AsiakirjaItemProjection
import fi.elsapalvelu.elsa.service.projection.AsiakirjaListProjection
import org.hibernate.engine.jdbc.BlobProxy
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class AsiakirjaServiceImpl(
    private val asiakirjaRepository: AsiakirjaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val asiakirjaMapper: AsiakirjaMapper
) : AsiakirjaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(
        asiakirjat: List<AsiakirjaDTO>,
        userId: String,
        tyoskentelyJaksoId: Long?
    ): List<AsiakirjaDTO>? {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
            asiakirjat.forEach {
                it.erikoistuvaLaakariId = kirjautunutErikoistuvaLaakari.id
                it.lisattypvm = LocalDateTime.now()
            }

            var asiakirjaEntities = asiakirjat.map {
                asiakirjaMapper.toEntity(it).apply {
                    data = BlobProxy.generateProxy(it.fileInputStream, it.fileSize!!)
                }
            }

            asiakirjaEntities = asiakirjaRepository.saveAll(asiakirjaEntities)

            return asiakirjaEntities.map { asiakirjaMapper.toDto(it) }
        }

        log.error("Creating Asiakirjat failed. User id with id $userId was not found.")

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariUserId(userId: String): MutableList<AsiakirjaListProjection> {
        return asiakirjaRepository.findAllByErikoistuvaLaakari(userId).toMutableList()
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariIdAndTyoskentelyjaksoId(
        userId: String,
        tyoskentelyJaksoId: Long?
    ): MutableList<AsiakirjaListProjection> {
        return asiakirjaRepository.findAllByErikoistuvaLaakariAndTyoskentelyjakso(userId, tyoskentelyJaksoId)
            .toMutableList()
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): AsiakirjaItemProjection? {
        val asiakirja = asiakirjaRepository.findAsiakirjaByIdAndErikoistuvaLaakari(userId, id)
        if (asiakirja == null) {
            log.error("Asiakirja $id requested by user $userId was not found.")
        }
        return asiakirja
    }


    override fun delete(id: Long, userId: String) {
        asiakirjaRepository.findByIdOrNull(id)?.let { asiakirja ->
            asiakirja.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (kirjautunutErikoistuvaLaakari == it) {
                        asiakirjaRepository.deleteById(id)
                    }
                }
            }
        }
    }

    override fun delete(ids: List<Long>, userId: String) {
        asiakirjaRepository.findAllById(ids.toMutableList()).let { asiakirjaRepository.deleteAll(it) }
    }

    override fun removeTyoskentelyjaksoReference(userId: String, tyoskentelyJaksoId: Long?) {
        val asiakirjaIdsByTyoskentelyjakso =
            asiakirjaRepository.findAllByErikoistuvaLaakariAndTyoskentelyjakso(userId, tyoskentelyJaksoId).map { it.id }
        val asiakirjaEntitiesByTyoskentelyjakso = asiakirjaRepository.findAllById(asiakirjaIdsByTyoskentelyjakso)
        asiakirjaEntitiesByTyoskentelyjakso.forEach {
            it.tyoskentelyjakso = null
        }

        asiakirjaRepository.saveAll(asiakirjaEntitiesByTyoskentelyjakso)
    }
}
