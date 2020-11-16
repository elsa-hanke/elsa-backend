package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OppimistavoiteRepository
import fi.elsapalvelu.elsa.service.OppimistavoiteService
import fi.elsapalvelu.elsa.service.dto.OppimistavoiteDTO
import fi.elsapalvelu.elsa.service.mapper.OppimistavoiteMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class OppimistavoiteServiceImpl(
    private val oppimistavoiteRepository: OppimistavoiteRepository,
    private val oppimistavoiteMapper: OppimistavoiteMapper
) : OppimistavoiteService {

    override fun save(
        oppimistavoiteDTO: OppimistavoiteDTO
    ): OppimistavoiteDTO {
        var oppimistavoite = oppimistavoiteMapper.toEntity(oppimistavoiteDTO)
        oppimistavoite = oppimistavoiteRepository.save(oppimistavoite)
        return oppimistavoiteMapper.toDto(oppimistavoite)
    }

    @Transactional(readOnly = true)
    override fun findAll(
        pageable: Pageable
    ): Page<OppimistavoiteDTO> {
        return oppimistavoiteRepository.findAll(pageable)
            .map(oppimistavoiteMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long
    ): Optional<OppimistavoiteDTO> {
        return oppimistavoiteRepository.findById(id)
            .map(oppimistavoiteMapper::toDto)
    }

    override fun delete(
        id: Long
    ) {
        oppimistavoiteRepository.deleteById(id)
    }
}
