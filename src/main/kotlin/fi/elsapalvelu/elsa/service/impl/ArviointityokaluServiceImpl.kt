package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArviointityokaluRepository
import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class ArviointityokaluServiceImpl(
    private val arviointityokaluRepository: ArviointityokaluRepository,
    private val arviointityokaluMapper: ArviointityokaluMapper
) : ArviointityokaluService {

    override fun save(arviointityokaluDTO: ArviointityokaluDTO): ArviointityokaluDTO {
        var arviointityokalu = arviointityokaluMapper.toEntity(arviointityokaluDTO)
        arviointityokalu = arviointityokaluRepository.save(arviointityokalu)
        return arviointityokaluMapper.toDto(arviointityokalu)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<ArviointityokaluDTO> {
        return arviointityokaluRepository.findAll()
            .mapTo(mutableListOf(), arviointityokaluMapper::toDto)
    }

    override fun findAllByKayttajaUserLogin(userId: String): MutableList<ArviointityokaluDTO> {
        return arviointityokaluRepository.findAllByKayttajaIsNullOrKayttajaUserLogin(userId)
            .mapTo(mutableListOf(), arviointityokaluMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArviointityokaluDTO> {
        return arviointityokaluRepository.findById(id)
            .map(arviointityokaluMapper::toDto)
    }

    override fun delete(id: Long) {
        arviointityokaluRepository.deleteById(id)
    }
}
