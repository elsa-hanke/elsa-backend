package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.AsiakirjaRepository
import fi.elsapalvelu.elsa.repository.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
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
    private val asiakirjaMapper: AsiakirjaMapper,
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val opintooikeusRepository: OpintooikeusRepository
) : AsiakirjaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun create(
        asiakirjat: List<AsiakirjaDTO>,
        opintooikeusId: Long,
        tyoskentelyJaksoId: Long?
    ): List<AsiakirjaDTO>? {
        opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let { opintooikeus ->
            var asiakirjaEntities = asiakirjat.map {
                asiakirjaMapper.toEntity(it).apply {
                    this.lisattypvm = LocalDateTime.now()
                    this.opintooikeus = opintooikeus
                    this.asiakirjaData?.data =
                        BlobProxy.generateProxy(it.asiakirjaData?.fileInputStream, it.asiakirjaData?.fileSize!!)
                }
            }

            asiakirjaEntities = asiakirjaRepository.saveAll(asiakirjaEntities)
            return asiakirjaEntities.map { asiakirjaMapper.toDto(it) }
        }

        return null
    }

    @Transactional(readOnly = true)
    override fun findAllByOpintooikeusId(opintooikeusId: Long): List<AsiakirjaDTO> {
        return asiakirjaRepository.findAllByOpintooikeusId(opintooikeusId)
            .map(asiakirjaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByOpintooikeusIdAndTyoskentelyjaksoId(
        opintooikeusId: Long,
        tyoskentelyJaksoId: Long?
    ): List<AsiakirjaDTO> {
        return asiakirjaRepository.findAllByOpintooikeusIdAndTyoskentelyjaksoId(
            opintooikeusId,
            tyoskentelyJaksoId
        )
            .map(asiakirjaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, opintooikeusId: Long): AsiakirjaDTO? {
        asiakirjaRepository.findOneByIdAndOpintooikeusId(id, opintooikeusId)?.let {
            return asiakirjaMapper.toDto(it).apply {
                asiakirjaData?.fileInputStream = it.asiakirjaData?.data?.binaryStream
            }
        }
        return null
    }

    override fun delete(id: Long, opintooikeusId: Long) {
        asiakirjaRepository.findOneByIdAndOpintooikeusId(id, opintooikeusId)?.let {
            deleteKoulutussuunnitelmaReferenceIfExists(id)
            asiakirjaRepository.deleteById(id)
        }
    }

    private fun deleteKoulutussuunnitelmaReferenceIfExists(asiakirjaId: Long) {
        koulutussuunnitelmaRepository.findOneByKoulutussuunnitelmaAsiakirjaIdOrMotivaatiokirjeAsiakirjaId(asiakirjaId)
            ?.let {
                if (it.koulutussuunnitelmaAsiakirja?.id == asiakirjaId) {
                    it.koulutussuunnitelmaAsiakirja = null
                } else if (it.motivaatiokirjeAsiakirja?.id == asiakirjaId) {
                    it.motivaatiokirjeAsiakirja = null
                }
            }
    }

    override fun delete(ids: List<Long>, opintooikeusId: Long) {
        asiakirjaRepository.findAllById(ids).filter {
            it.opintooikeus?.id == opintooikeusId
        }.let { asiakirjaRepository.deleteAll(it) }
    }

    override fun removeTyoskentelyjaksoReference(tyoskentelyJaksoId: Long?) {
        val asiakirjaIdsByTyoskentelyjakso =
            asiakirjaRepository.findAllByTyoskentelyjaksoId(tyoskentelyJaksoId).map { it.id }
        val asiakirjaEntitiesByTyoskentelyjakso = asiakirjaRepository.findAllById(asiakirjaIdsByTyoskentelyjakso)
        asiakirjaEntitiesByTyoskentelyjakso.forEach {
            it.tyoskentelyjakso = null
        }

        asiakirjaRepository.saveAll(asiakirjaEntitiesByTyoskentelyjakso)
    }
}
