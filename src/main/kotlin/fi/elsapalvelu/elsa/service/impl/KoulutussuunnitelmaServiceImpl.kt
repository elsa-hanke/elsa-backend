package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.service.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.dto.KoulutussuunnitelmaDTO
import fi.elsapalvelu.elsa.service.mapper.KoulutussuunnitelmaMapper
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class KoulutussuunnitelmaServiceImpl(
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val koulutussuunnitelmaMapper: KoulutussuunnitelmaMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
) : KoulutussuunnitelmaService {

    override fun save(
        koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        userId: String
    ): KoulutussuunnitelmaDTO? {
        return erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuvaLaakari ->
            if (koulutussuunnitelmaDTO.erikoistuvaLaakariId == erikoistuvaLaakari.id) {
                koulutussuunnitelmaRepository.findOneByErikoistuvaLaakariId(erikoistuvaLaakari.id!!)
                    ?.let { existingKoulutussuunnitelma ->
                        koulutussuunnitelmaDTO.id = existingKoulutussuunnitelma.id
                        if (koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirjaUpdated) {
                            koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja
                        }

                        var koulutussuunnitelma = koulutussuunnitelmaMapper.toEntity(koulutussuunnitelmaDTO)

                        // Päivitetään koulutussuunnitelma jos asiakirja muuttunut
                        if (koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirjaUpdated) {
                            koulutussuunnitelma.koulutussuunnitelmaAsiakirja?.erikoistuvaLaakari = erikoistuvaLaakari
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
                            koulutussuunnitelma.motivaatiokirjeAsiakirja?.erikoistuvaLaakari = erikoistuvaLaakari
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
            } else {
                null
            }
        }
    }

    override fun findOneByErikoistuvaLaakariKayttajaUserId(userId: String): KoulutussuunnitelmaDTO? {
        return erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuvaLaakari ->
            koulutussuunnitelmaMapper.toDto(
                koulutussuunnitelmaRepository.findOneByErikoistuvaLaakariId(erikoistuvaLaakari.id!!)
                    ?: koulutussuunnitelmaRepository.save(
                        Koulutussuunnitelma(erikoistuvaLaakari = erikoistuvaLaakari)
                    )
            )
        }
    }

}