package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.repository.koulutus.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.koulutus.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutussuunnitelmaDTO
import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutussuunnitelmaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class KoulutussuunnitelmaServiceImpl(
    private val koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository,
    private val koulutussuunnitelmaMapper: KoulutussuunnitelmaMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val pdfTextFieldValidator: PdfTextFieldValidator
) : KoulutussuunnitelmaService {

    override fun save(
        koulutussuunnitelmaDTO: KoulutussuunnitelmaDTO,
        opintooikeusId: Long
    ): KoulutussuunnitelmaDTO? {
        pdfTextFieldValidator.validate(
            fields = listOf(
                "motivaatiokirje" to koulutussuunnitelmaDTO.motivaatiokirje,
                "opiskelu-ja-tyohistoria" to koulutussuunnitelmaDTO.opiskeluJaTyohistoria,
                "vahvuudet" to koulutussuunnitelmaDTO.vahvuudet,
                "tulevaisuuden-visiointi" to koulutussuunnitelmaDTO.tulevaisuudenVisiointi,
                "osaamisen-kartuttaminen" to koulutussuunnitelmaDTO.osaamisenKartuttaminen,
                "elamankentta" to koulutussuunnitelmaDTO.elamankentta,
                "motivaatiokirje-tiedostonimi" to
                    koulutussuunnitelmaDTO.motivaatiokirjeAsiakirja?.nimi
            ),
            pdfSource = "koulutussuunnitelma",
            sourceId = koulutussuunnitelmaDTO.id
        )
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
                        koulutussuunnitelmaDTO.koulutussuunnitelmaAsiakirja?.asiakirjaData?.fileInputStream?.readAllBytes()
                    koulutussuunnitelma.koulutussuunnitelmaAsiakirja?.lisattypvm = LocalDateTime.now()
                } else {
                    koulutussuunnitelma.koulutussuunnitelmaAsiakirja =
                        existingKoulutussuunnitelma.koulutussuunnitelmaAsiakirja
                }

                // Päivitetään motivaatiokirje jos asiakirja muuttunut
                if (koulutussuunnitelmaDTO.motivaatiokirjeAsiakirjaUpdated) {
                    koulutussuunnitelma.motivaatiokirjeAsiakirja?.opintooikeus = koulutussuunnitelma.opintooikeus
                    koulutussuunnitelma.motivaatiokirjeAsiakirja?.asiakirjaData?.data =
                        koulutussuunnitelmaDTO.motivaatiokirjeAsiakirja?.asiakirjaData?.fileInputStream?.readAllBytes()
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
        return koulutussuunnitelmaRepository.findOneByOpintooikeusId(opintooikeusId)?.let {
            koulutussuunnitelmaMapper.toDto(it)
        } ?: opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
                val koulutussuunnitelma = koulutussuunnitelmaRepository.save(Koulutussuunnitelma(opintooikeus = it))
                koulutussuunnitelmaMapper.toDto(koulutussuunnitelma)
        }
    }
}
