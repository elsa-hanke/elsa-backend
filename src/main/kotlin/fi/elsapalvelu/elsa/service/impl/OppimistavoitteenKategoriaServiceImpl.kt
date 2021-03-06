package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OppimistavoitteenKategoriaRepository
import fi.elsapalvelu.elsa.service.OppimistavoitteenKategoriaService
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteenKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.OppimistavoitteenKategoriaMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class OppimistavoitteenKategoriaServiceImpl(
    private val oppimistavoitteenKategoriaRepository: OppimistavoitteenKategoriaRepository,
    private val oppimistavoitteenKategoriaMapper: OppimistavoitteenKategoriaMapper
) : OppimistavoitteenKategoriaService {

    override fun save(
        oppimistavoitteenKategoriaDTO: OppimistavoitteenKategoriaDTO
    ): OppimistavoitteenKategoriaDTO {
        var oppimistavoitteenKategoria = oppimistavoitteenKategoriaMapper.toEntity(oppimistavoitteenKategoriaDTO)
        oppimistavoitteenKategoria = oppimistavoitteenKategoriaRepository.save(oppimistavoitteenKategoria)
        return oppimistavoitteenKategoriaMapper.toDto(oppimistavoitteenKategoria)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<OppimistavoitteenKategoriaDTO> {
        return oppimistavoitteenKategoriaRepository.findAll()
            .map(oppimistavoitteenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAll(
        pageable: Pageable
    ): Page<OppimistavoitteenKategoriaDTO> {
        return oppimistavoitteenKategoriaRepository.findAll(pageable)
            .map(oppimistavoitteenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoisalaId(id: Long): List<OppimistavoitteenKategoriaDTO> {
        return oppimistavoitteenKategoriaRepository.findAllByErikoisalaId(id)
            .map(oppimistavoitteenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<OppimistavoitteenKategoriaDTO> {
        return oppimistavoitteenKategoriaRepository.findById(id)
            .map(oppimistavoitteenKategoriaMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        oppimistavoitteenKategoriaRepository.deleteById(id)
    }
}
