package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.IlmoitusRepository
import fi.elsapalvelu.elsa.service.IlmoitusService
import fi.elsapalvelu.elsa.service.dto.IlmoitusDTO
import fi.elsapalvelu.elsa.service.mapper.IlmoitusMapper
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
        return ilmoitusRepository.findAllByOrderById()
            .map(ilmoitusMapper::toDto)
    }

    override fun findOne(id: Long): IlmoitusDTO? {
        return ilmoitusRepository.findByIdOrNull(id)?.let {
            return ilmoitusMapper.toDto(it)
        }
    }

    override fun create(ilmoitusDTO: IlmoitusDTO): IlmoitusDTO {
        val ilmoitus = ilmoitusMapper.toEntity(ilmoitusDTO)
        ilmoitusRepository.save(ilmoitus)
        return ilmoitusMapper.toDto(ilmoitus)
    }

    override fun update(ilmoitusDTO: IlmoitusDTO): IlmoitusDTO? {
        val ilmoitus = ilmoitusRepository.findByIdOrNull(ilmoitusDTO.id)
        ilmoitus?.let {
            it.teksti = ilmoitusDTO.teksti
            ilmoitusRepository.save(it)
            return ilmoitusMapper.toDto(ilmoitus)
        }
        return null
    }

    override fun delete(id: Long) {
        ilmoitusRepository.deleteById(id)
    }
}
