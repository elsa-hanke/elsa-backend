package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.PaivakirjaAihekategoriaRepository
import fi.elsapalvelu.elsa.service.PaivakirjaAihekategoriaService
import fi.elsapalvelu.elsa.service.dto.PaivakirjaAihekategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.PaivakirjaAihekategoriaMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class PaivakirjaAihekategoriaServiceImpl(
    private val paivakirjaAihekategoriaRepository: PaivakirjaAihekategoriaRepository,
    private val paivakirjaAihekategoriaMapper: PaivakirjaAihekategoriaMapper
) : PaivakirjaAihekategoriaService {

    @Transactional(readOnly = true)
    override fun findAll(): List<PaivakirjaAihekategoriaDTO> {
        return paivakirjaAihekategoriaRepository.findAll()
            .map(paivakirjaAihekategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<PaivakirjaAihekategoriaDTO> {
        return paivakirjaAihekategoriaRepository.findById(id)
            .map(paivakirjaAihekategoriaMapper::toDto)
    }
}
