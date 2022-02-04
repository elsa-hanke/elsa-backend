package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OpintoopasRepository
import fi.elsapalvelu.elsa.service.OpintoopasService
import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO
import fi.elsapalvelu.elsa.service.mapper.OpintoopasMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OpintoopasServiceImpl(
    private val opintoopasRepository: OpintoopasRepository,
    private val opintoopasMapper: OpintoopasMapper
) : OpintoopasService {

    override fun findOne(id: Long): OpintoopasDTO? {
        return opintoopasRepository.findByIdOrNull(id)?.let {
            return opintoopasMapper.toDto(it)
        }
    }

    override fun findAll(): List<OpintoopasDTO> {
        return opintoopasRepository.findAll()
            .map(opintoopasMapper::toDto)
    }
}
