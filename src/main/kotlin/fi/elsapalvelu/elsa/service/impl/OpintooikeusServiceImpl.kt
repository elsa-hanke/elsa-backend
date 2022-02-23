package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.mapper.OpintooikeusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityNotFoundException

private const val ENTITY_NOT_FOUND_ERROR_MSG = "Opinto-oikeutta ei löydy"

@Service
@Transactional
class OpintooikeusServiceImpl(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintooikeusMapper: OpintooikeusMapper
) : OpintooikeusService {
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO> {
        return opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId).map(opintooikeusMapper::toDto)
    }

    override fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusDTO {
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(userId)?.let {
            return opintooikeusMapper.toDto(it)
        }

        throw EntityNotFoundException(ENTITY_NOT_FOUND_ERROR_MSG)
    }

    override fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): Long {
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(userId)?.let {
            return it.id!!
        }

        throw EntityNotFoundException(ENTITY_NOT_FOUND_ERROR_MSG)
    }

    override fun onOikeus(user: User): Boolean {
        if (user.authorities.contains(Authority(name = ERIKOISTUVA_LAAKARI))) {
            return opintooikeusRepository.existsByErikoistuvaLaakariKayttajaUserId(
                user.id!!,
                LocalDate.now()
            )
        }
        return true
    }
}
