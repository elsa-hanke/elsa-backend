package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.repository.koulutus.KoulutusjaksoRepository
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.arviointi.ArvioitavaKokonaisuusService
import fi.elsapalvelu.elsa.service.koulutus.KoulutusjaksoService
import fi.elsapalvelu.elsa.service.koulutus.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutusjaksoDTO
import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutusjaksoMapper
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
    private val pdfTextFieldValidator: PdfTextFieldValidator
) : KoulutusjaksoService {

    override fun save(
        koulutusjaksoDTO: KoulutusjaksoDTO,
        opintooikeusId: Long
    ): KoulutusjaksoDTO? {
        pdfTextFieldValidator.validate(
            fields = listOf(
                "koulutusjakson-nimi" to koulutusjaksoDTO.nimi,
                "muut-osaamistavoitteet" to koulutusjaksoDTO.muutOsaamistavoitteet
            ),
            pdfSource = "koulutusjakso",
            sourceId = koulutusjaksoDTO.id
        )
        return koulutussuunnitelmaService.findOneByOpintooikeusId(opintooikeusId)?.let { koulutussuunnitelmaDTO ->
            koulutusjaksoDTO.koulutussuunnitelma = koulutussuunnitelmaDTO

            koulutusjaksoDTO.tyoskentelyjaksot.filter {
                it.id?.let { tyoskentelyjaksoId ->
                    tyoskentelyjaksoService.findOne(tyoskentelyjaksoId, opintooikeusId)
                }?.let { true } == true
            }

            val arvioitavatKokonaisuudet =
                arvioitavaKokonaisuusService.findAllByOpintooikeusId(opintooikeusId)
            koulutusjaksoDTO.osaamistavoitteet.filter {
                arvioitavatKokonaisuudet.any { kokonaisuus ->
                    kokonaisuus.id == it.id
                }
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

    override fun removeTyoskentelyjaksoReference(tyoskentelyJaksoId: Long) {
        val koulutusjaksotByTyoskentelyjakso =
            koulutusjaksoRepository.findAllByTyoskentelyjaksoId(tyoskentelyJaksoId)

        koulutusjaksotByTyoskentelyjakso.forEach { koulutusjakso ->
            koulutusjakso.tyoskentelyjaksot?.removeIf { it.id == tyoskentelyJaksoId }
        }
    }
}
