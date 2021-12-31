package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OpintoopasRepository
import fi.elsapalvelu.elsa.service.OpintoopasService
import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO
import fi.elsapalvelu.elsa.service.mapper.OpintoopasMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OpintoopasServiceImpl(
    private val opintoopasRepository: OpintoopasRepository,
    private val opintoopasMapper: OpintoopasMapper
) : OpintoopasService {

    override fun findAllByOpintooikeudetErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintoopasDTO> {
        return opintoopasRepository.findAllByOpintooikeudetErikoistuvaLaakariKayttajaUserId(userId)
            .map(opintoopasMapper::toDto)
    }
}
