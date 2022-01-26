package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.repository.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO
import fi.elsapalvelu.elsa.service.mapper.KoulutussuunnitelmaMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class KoulutussuunnitelmaServiceImpl(
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val koulutussuunnitelmaMapper: KoulutussuunnitelmaMapper,
    private val opintooikeusRepository: OpintooikeusRepository
) : KoulutussuunnitelmaService {

    override fun save(
        koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        opintooikeusId: Long
    ): KoulutussuunnitelmaDTO? {
        return koulutussuunnitelmaRepository.findOneByOpintooikeusId(opintooikeusId)
            ?.let { existingKoulutussuunnitelma ->
                koulutussuunnitelmaDTO.id = existingKoulutussuunnitelma.id
                if (koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirjaUpdated) {
                    koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja
                }

                var koulutussuunnitelma = koulutussuunnitelmaMapper.toEntity(koulutussuunnitelmaDTO)
                koulutussuunnitelma.opintooikeus = existingKoulutussuunnitelma.opintooikeus

                // Päivitetään koulutussuunnitelma jos asiakirja muuttunut
                if (koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirjaUpdated) {
                    koulutussuunnitelma.koulutussuunnitelmaAsiakirja?.opintooikeus = koulutussuunnitelma.opintooikeus
                    koulutussuunnitelma.koulutussuunnitelmaAsiakirja?.asiakirjaData?.data =
                        BlobProxy.generateProxy(
                            koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja?.asiakirjaData?.fileInputStream,
                            koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja?.asiakirjaData?.fileSize!!
                        )
                    koulutussuunnitelma.koulutussuunnitelmaAsiakirja?.lisattypvm = LocalDateTime.now()
                } else {
                    koulutussuunnitelma.koulutussuunnitelmaAsiakirja =
                        existingKoulutussuunnitelma.koulutussuunnitelmaAsiakirja
                }

                // Päivitetään motivaatiokirje jos asiakirja muuttunut
                if (koulutussuunnitelmaDTO.motivaatiokirjeAsiakirjaUpdated) {
                    koulutussuunnitelma.motivaatiokirjeAsiakirja?.opintooikeus = koulutussuunnitelma.opintooikeus
                    koulutussuunnitelma.motivaatiokirjeAsiakirja?.asiakirjaData?.data =
                        BlobProxy.generateProxy(
                            koulutussuunnitelmaDTO.motivaatiokirjeAsiakirja?.asiakirjaData?.fileInputStream,
                            koulutussuunnitelmaDTO.motivaatiokirjeAsiakirja?.asiakirjaData?.fileSize!!
                        )
                    koulutussuunnitelma.motivaatiokirjeAsiakirja?.lisattypvm = LocalDateTime.now()
                } else {
                    koulutussuunnitelma.motivaatiokirjeAsiakirja =
                        existingKoulutussuunnitelma.motivaatiokirjeAsiakirja
                }

                koulutussuunnitelma = koulutussuunnitelmaRepository.save(koulutussuunnitelma)

                koulutussuunnitelmaMapper.toDto(koulutussuunnitelma)
            }
    }

    override fun findOneByOpintooikeusId(opintooikeusId: Long): KoulutussuunnitelmaDTO? {
        koulutussuunnitelmaRepository.findOneByOpintooikeusId(opintooikeusId)?.let {
            return koulutussuunnitelmaMapper.toDto(it)
        } ?: run {
            return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
                val koulutussuunnitelma = koulutussuunnitelmaRepository.save(Koulutussuunnitelma(opintooikeus = it))
                return koulutussuunnitelmaMapper.toDto(koulutussuunnitelma)
            }
        }
    }
}
