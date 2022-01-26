package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KoulutusjaksoRepository
import fi.elsapalvelu.elsa.service.ArvioitavaKokonaisuusService
import fi.elsapalvelu.elsa.service.KoulutusjaksoService
import fi.elsapalvelu.elsa.service.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.KoulutusjaksoDTO
import fi.elsapalvelu.elsa.service.mapper.KoulutusjaksoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KoulutusjaksoServiceImpl(
    private val koulutusjaksoRepository: KoulutusjaksoRepository,
    private val koulutusjaksoMapper: KoulutusjaksoMapper,
    private val koulutussuunnitelmaService: KoulutussuunnitelmaService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val arvioitavaKokonaisuusService: ArvioitavaKokonaisuusService,
) : KoulutusjaksoService {

    override fun save(
        koulutusjaksoDTO: KoulutusjaksoDTO,
        opintooikeusId: Long
    ): KoulutusjaksoDTO? {
        return koulutussuunnitelmaService.findOneByOpintooikeusId(opintooikeusId)?.let { koulutussuunnitelmaDTO ->
            koulutusjaksoDTO.koulutussuunnitelma = koulutussuunnitelmaDTO

            koulutusjaksoDTO.tyoskentelyjaksot.filter {
                it.id?.let { tyoskentelyjaksoId ->
                    tyoskentelyjaksoService.findOne(tyoskentelyjaksoId, opintooikeusId)
                }?.let { true } ?: false
            }

            val osaamistavoitteetErikoisalalla =
                arvioitavaKokonaisuusService.findAllByOpintooikeusId(opintooikeusId)
            koulutusjaksoDTO.osaamistavoitteet.filter {
                osaamistavoitteetErikoisalalla.find { osaamistavoiteId ->
                    osaamistavoiteId.id === it.id
                }?.let { true } ?: false
            }

            var koulutusjakso = koulutusjaksoMapper.toEntity(koulutusjaksoDTO)
            koulutusjakso = koulutusjaksoRepository.save(koulutusjakso)
            return koulutusjaksoMapper.toDto(koulutusjakso)
        }
    }

    @Transactional(readOnly = true)
    override fun findAllByKoulutussuunnitelmaOpintooikeusId(
        opintooikeusId: Long
    ): List<KoulutusjaksoDTO> {
        return koulutusjaksoRepository.findAllByKoulutussuunnitelmaOpintooikeusId(
            opintooikeusId
        ).map(koulutusjaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long,
        opintooikeusId: Long
    ): KoulutusjaksoDTO? {
        return koulutusjaksoRepository
            .findOneByIdAndKoulutussuunnitelmaOpintooikeusId(id, opintooikeusId)?.let {
                koulutusjaksoMapper.toDto(it)
            }
    }

    override fun findForSeurantajakso(ids: List<Long>, opintooikeusId: Long): List<KoulutusjaksoDTO> {
        return koulutusjaksoRepository.findForSeurantajakso(ids, opintooikeusId)
            .map(koulutusjaksoMapper::toDto)
    }

    override fun delete(
        id: Long,
        opintooikeusId: Long
    ) {
        koulutusjaksoRepository.deleteByIdAndKoulutussuunnitelmaOpintooikeusId(
            id,
            opintooikeusId
        )
    }
}
