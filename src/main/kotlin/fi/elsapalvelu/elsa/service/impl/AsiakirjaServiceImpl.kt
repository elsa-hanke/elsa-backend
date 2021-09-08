package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.AsiakirjaService
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.mapper.AsiakirjaMapper
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
        kayttajaId: String,
        tyoskentelyJaksoId: Long?
    ): List<AsiakirjaDTO>? {
        erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)?.let { kirjautunutErikoistuvaLaakari ->
            asiakirjat.forEach {
                it.erikoistuvaLaakariId = kirjautunutErikoistuvaLaakari.id
                it.lisattypvm = LocalDateTime.now()
            }

            var asiakirjaEntities = asiakirjat.map {
                asiakirjaMapper.toEntity(it).apply {
                    this.asiakirjaData?.data =
                        BlobProxy.generateProxy(it.asiakirjaData?.fileInputStream, it.asiakirjaData?.fileSize!!)
                }
            }

            asiakirjaEntities = asiakirjaRepository.saveAll(asiakirjaEntities)

            return asiakirjaEntities.map { asiakirjaMapper.toDto(it) }
        }

        log.error("Creating Asiakirjat failed. User id with id $kayttajaId was not found.")

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariId(kayttajaId: String): List<AsiakirjaDTO> {
        return asiakirjaRepository.findAllByErikoistuvaLaakariKayttajaId(kayttajaId)
            .map(asiakirjaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariIdAndTyoskentelyjaksoId(
        kayttajaId: String,
        tyoskentelyJaksoId: Long?
    ): List<AsiakirjaDTO> {
        return asiakirjaRepository.findAllByErikoistuvaLaakariKayttajaIdAndTyoskentelyjaksoId(
            kayttajaId,
            tyoskentelyJaksoId
        )
            .map(asiakirjaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, kayttajaId: String): AsiakirjaDTO? {
        asiakirjaRepository.findOneByIdAndErikoistuvaLaakariKayttajaId(id, kayttajaId).let {
            return asiakirjaMapper.toDto(it).apply {
                asiakirjaData?.fileInputStream = it.asiakirjaData?.data?.binaryStream
            }
        }
    }

    override fun delete(id: Long, kayttajaId: String) {
        asiakirjaRepository.findByIdOrNull(id)?.let { asiakirja ->
            asiakirja.erikoistuvaLaakari.let {
                erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)?.let { kirjautunutErikoistuvaLaakari ->
                    if (kirjautunutErikoistuvaLaakari == it) {
                        asiakirjaRepository.deleteById(id)
                    }
                }
            }
        }
    }

    override fun delete(ids: List<Long>, kayttajaId: String) {
        asiakirjaRepository.findAllById(ids).let { asiakirjaRepository.deleteAll(it) }
    }

    override fun removeTyoskentelyjaksoReference(kayttajaId: String, tyoskentelyJaksoId: Long?) {
        val asiakirjaIdsByTyoskentelyjakso =
            asiakirjaRepository.findAllByErikoistuvaLaakariKayttajaIdAndTyoskentelyjaksoId(
                kayttajaId,
                tyoskentelyJaksoId
            ).map { it.id }
        val asiakirjaEntitiesByTyoskentelyjakso = asiakirjaRepository.findAllById(asiakirjaIdsByTyoskentelyjakso)
        asiakirjaEntitiesByTyoskentelyjakso.forEach {
            it.tyoskentelyjakso = null
        }

        asiakirjaRepository.saveAll(asiakirjaEntitiesByTyoskentelyjakso)
    }
}
