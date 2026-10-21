package fi.elsapalvelu.elsa.service.impl.seuranta

import fi.elsapalvelu.elsa.domain.seuranta.Ilmoitus
import fi.elsapalvelu.elsa.repository.seuranta.IlmoitusRepository
import fi.elsapalvelu.elsa.service.seuranta.IlmoitusHtmlSanitizer
import fi.elsapalvelu.elsa.service.seuranta.IlmoitusService
import fi.elsapalvelu.elsa.service.dto.seuranta.IlmoitusDTO
import fi.elsapalvelu.elsa.service.mapper.seuranta.IlmoitusMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class IlmoitusServiceImpl(
    private val ilmoitusRepository: IlmoitusRepository,
    private val ilmoitusMapper: IlmoitusMapper
) : IlmoitusService {

    override fun findAll(): List<IlmoitusDTO> {
        return ilmoitusRepository.findAllByOrderByIdDesc()
            .map { toSafeDto(it) }
    }

    override fun findOne(id: Long): IlmoitusDTO? {
        return ilmoitusRepository.findByIdOrNull(id)?.let {
            return toSafeDto(it)
        }
    }

    override fun create(ilmoitusDTO: IlmoitusDTO): IlmoitusDTO {
        val ilmoitus = ilmoitusMapper.toEntity(ilmoitusDTO.copy(teksti = IlmoitusHtmlSanitizer.sanitize(ilmoitusDTO.teksti)))
        ilmoitusRepository.save(ilmoitus)
        return ilmoitusMapper.toDto(ilmoitus)
    }

    override fun update(ilmoitusDTO: IlmoitusDTO): IlmoitusDTO? {
        val ilmoitus = ilmoitusDTO.id?.let { ilmoitusRepository.findByIdOrNull(it) }
        ilmoitus?.let {
            it.teksti = IlmoitusHtmlSanitizer.sanitize(ilmoitusDTO.teksti)
            ilmoitusRepository.save(it)
            return ilmoitusMapper.toDto(ilmoitus)
        }
        return null
    }

    // Also protect historical records when loaded into the editor, without rewriting the database.
    private fun toSafeDto(ilmoitus: Ilmoitus): IlmoitusDTO =
        ilmoitusMapper.toDto(ilmoitus).apply { teksti = IlmoitusHtmlSanitizer.sanitize(teksti) }

    override fun delete(id: Long) {
        ilmoitusRepository.deleteById(id)
    }
}
