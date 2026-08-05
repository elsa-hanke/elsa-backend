package fi.elsapalvelu.elsa.service.impl.arviointi

import fi.elsapalvelu.elsa.repository.arviointi.ArviointiasteikkoRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.arviointi.ArviointiasteikkoService
import fi.elsapalvelu.elsa.service.dto.ArviointiasteikkoDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointiasteikkoMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArviointiasteikkoServiceImpl(
    private val arviointiasteikkoMapper: ArviointiasteikkoMapper,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val arviointiasteikkoRepository: ArviointiasteikkoRepository
) : ArviointiasteikkoService {

    @Transactional(readOnly = true)
    override fun findByOpintooikeusId(opintooikeusId: Long): ArviointiasteikkoDTO? {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            it.opintoopas?.arviointiasteikko?.let { arviointiasteikko ->
                arviointiasteikkoMapper.toDto(arviointiasteikko)
            }
        }
    }

    override fun findAll(): List<ArviointiasteikkoDTO> {
        return arviointiasteikkoRepository.findAllWithTasot().map(arviointiasteikkoMapper::toDto)
    }
}
