package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.PaivakirjamerkintaService
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.PaivakirjamerkintaMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PaivakirjamerkintaServiceImpl(
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val paivakirjamerkintaMapper: PaivakirjamerkintaMapper,
    private val opintooikeusRepository: OpintooikeusRepository
) : PaivakirjamerkintaService {

    override fun save(paivakirjamerkintaDTO: PaivakirjamerkintaDTO, opintooikeusId: Long): PaivakirjamerkintaDTO? {
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
