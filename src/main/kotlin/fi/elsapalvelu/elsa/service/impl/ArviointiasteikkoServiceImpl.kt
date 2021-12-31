package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.ArviointiasteikkoService
import fi.elsapalvelu.elsa.service.dto.ArviointiasteikkoDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointiasteikkoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArviointiasteikkoServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val arviointiasteikkoMapper: ArviointiasteikkoMapper
): ArviointiasteikkoService {

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): ArviointiasteikkoDTO? {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val arviointiasteikko =
            kirjautunutErikoistuvaLaakari?.opintooikeudet?.firstOrNull()?.opintoopas?.arviointiasteikko
        return arviointiasteikko?.let { arviointiasteikkoMapper.toDto(it) }
    }
}
