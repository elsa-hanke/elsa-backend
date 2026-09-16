package fi.elsapalvelu.elsa.service.impl.seuranta

import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.seuranta.PaivakirjamerkintaService
import fi.elsapalvelu.elsa.service.dto.seuranta.PaivakirjamerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.seuranta.PaivakirjamerkintaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PaivakirjamerkintaServiceImpl(
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val paivakirjamerkintaMapper: PaivakirjamerkintaMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val pdfTextFieldValidator: PdfTextFieldValidator
) : PaivakirjamerkintaService {

    override fun save(paivakirjamerkintaDTO: PaivakirjamerkintaDTO, opintooikeusId: Long): PaivakirjamerkintaDTO? {
        pdfTextFieldValidator.validate(
            fields = listOf(
                "oppimistapahtuma" to paivakirjamerkintaDTO.oppimistapahtumanNimi,
                "muun-aiheen-nimi" to paivakirjamerkintaDTO.muunAiheenNimi,
                "ajatuksia-opitusta-ja-sen-soveltamisesta" to paivakirjamerkintaDTO.reflektio
            ),
            pdfSource = "paivakirjamerkinta",
            sourceId = paivakirjamerkintaDTO.id,
            sourceDate = paivakirjamerkintaDTO.paivamaara
        )
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            var paivakirjamerkinta = paivakirjamerkintaMapper.toEntity(paivakirjamerkintaDTO).apply {
                opintooikeus = it
            }
            paivakirjamerkinta = paivakirjamerkintaRepository.save(paivakirjamerkinta)
            paivakirjamerkintaMapper.toDto(paivakirjamerkinta)
        }
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, opintooikeusId: Long): PaivakirjamerkintaDTO? {
        return paivakirjamerkintaRepository.findOneByIdAndOpintooikeusId(id, opintooikeusId)?.let {
            paivakirjamerkintaMapper.toDto(it)
        }
    }

    override fun delete(id: Long, opintooikeusId: Long) {
        paivakirjamerkintaRepository.deleteByIdAndOpintooikeusId(id, opintooikeusId)
    }
}
