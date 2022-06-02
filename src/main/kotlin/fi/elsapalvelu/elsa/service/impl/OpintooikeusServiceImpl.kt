package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.mapper.OpintooikeusMapper
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import javax.persistence.EntityNotFoundException


private const val ENTITY_NOT_FOUND_ERROR_MSG = "Opinto-oikeutta ei löydy"

@Service
@Transactional
class OpintooikeusServiceImpl(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintooikeusMapper: OpintooikeusMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val clock: Clock
) : OpintooikeusService {
    override fun findAllValidByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO> {
        return opintooikeusRepository.findByErikoistuvaLaakariKayttajaUserIdAndBetweenDate(
            userId,
            LocalDate.now(clock)
        ).filter { allowedOpintooikeusTilat().contains(it.tila) }.map(opintooikeusMapper::toDto)
    }

    override fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): OpintooikeusDTO {
        getImpersonatedOpintooikeusId()?.let {
            return opintooikeusMapper.toDto(
                opintooikeusRepository.findById(it).get()
            )
        }
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(userId)
            ?.let {
                return opintooikeusMapper.toDto(it)
            }

        throw EntityNotFoundException(ENTITY_NOT_FOUND_ERROR_MSG)
    }

    override fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): Long {
        getImpersonatedOpintooikeusId()?.let { return it }
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(userId)
            ?.let {
                return it.id!!
            }

        throw EntityNotFoundException(ENTITY_NOT_FOUND_ERROR_MSG)
    }

    private fun getImpersonatedOpintooikeusId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal: Saml2AuthenticatedPrincipal =
            authentication.principal as Saml2AuthenticatedPrincipal
        val authorities = authentication.authorities.map { it.authority }
        if (authorities.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) ||
            authorities.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
        ) {
            return principal.getFirstAttribute("opintooikeusId") as Long
        }
        return null
    }

    override fun onOikeus(user: User): Boolean {
        opintooikeusRepository.findByErikoistuvaLaakariKayttajaUserIdAndBetweenDate(
            user.id!!,
            LocalDate.now(clock)
        ).forEach {
            if (allowedOpintooikeusTilat().contains(it.tila) && it.erikoisala?.liittynytElsaan == true) return true
        }
        return false
    }

    override fun setOpintooikeusKaytossa(userId: String, opintooikeusId: Long) {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuva ->
            opintooikeusRepository.findOneByIdAndErikoistuvaLaakariIdAndBetweenDate(
                opintooikeusId,
                erikoistuva.id!!,
                LocalDate.now(clock)
            )?.let { preferredOpintooikeus ->
                erikoistuva.opintooikeudet.forEach { opintooikeus ->
                    opintooikeus.kaytossa = false
                }
                preferredOpintooikeus.kaytossa = true
            }
        }
    }

    companion object {
        @JvmStatic
        fun allowedOpintooikeusTilat(): List<OpintooikeudenTila> = listOf(
            OpintooikeudenTila.AKTIIVINEN,
            OpintooikeudenTila.AKTIIVINEN_EI_LASNA,
            OpintooikeudenTila.PASSIIVINEN,
            OpintooikeudenTila.VALMISTUNUT
        )
    }
}
