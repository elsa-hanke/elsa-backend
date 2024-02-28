package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.OpintoopasRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.constants.OPINTOOIKEUS_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaOpintooikeusDTO
import fi.elsapalvelu.elsa.service.mapper.OpintooikeusMapper
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull

@Service
@Transactional
class OpintooikeusServiceImpl(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintooikeusMapper: OpintooikeusMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val clock: Clock,
    private val opintoopasRepository: OpintoopasRepository
) : OpintooikeusService {
    override fun findAllValidByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO> {
        return opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
            userId,
            LocalDate.now(clock),
            OpintooikeudenTila.allowedTilat(),
            OpintooikeudenTila.endedTilat()
        ).map(opintooikeusMapper::toDto)
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

        throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    override fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(userId: String): Long {
        getImpersonatedOpintooikeusId()?.let { return it }
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(userId)
            ?.let {
                return it.id!!
            }

        throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    override fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
        userId: String, erikoisalaId: Long
    ): Long {
        getImpersonatedOpintooikeusId()?.let { return it }
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrueAndErikoisalaId(userId, erikoisalaId)
            ?.let {
                return it.id!!
            }

        throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    override fun findAllByTerveyskoulutusjaksoSuorittamatta(): List<Opintooikeus> {
        return opintooikeusRepository.findAllByTerveyskoulutusjaksoSuorittamatta(
            LocalDate.now(clock), OpintooikeudenTila.allowedTilat()
        )
    }

    override fun onOikeus(user: User): Boolean {
        if (opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
                user.id!!,
                LocalDate.now(clock),
                OpintooikeudenTila.allowedTilat(),
                OpintooikeudenTila.endedTilat()
            ).any()
        ) {
            return true
        }

        return false
    }

    override fun checkOpintooikeusKaytossaValid(user: User) {
        val opintooikeusKaytossa =
            opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(user.id!!)
                ?: return
        if ((opintooikeusKaytossa.viimeinenKatselupaiva != null && LocalDate.now()
                .isAfter(opintooikeusKaytossa.viimeinenKatselupaiva))
            || opintooikeusKaytossa.tila == OpintooikeudenTila.VANHENTUNUT
            || opintooikeusKaytossa.erikoisala?.liittynytElsaan == false
        ) {
            opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
                user.id!!,
                LocalDate.now(clock),
                OpintooikeudenTila.allowedTilat(),
                OpintooikeudenTila.endedTilat()
            ).elementAtOrNull(0)?.let {
                opintooikeusKaytossa.kaytossa = false
                it.kaytossa = true
            }
        }
    }

    override fun setOpintooikeusKaytossa(userId: String, opintooikeusId: Long) {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuva ->
            opintooikeusRepository.findOneByIdAndErikoistuvaLaakariIdAndBetweenDate(
                opintooikeusId, erikoistuva.id!!, LocalDate.now(clock)
            )?.let { preferredOpintooikeus ->
                erikoistuva.opintooikeudet.forEach { opintooikeus ->
                    opintooikeus.kaytossa = false
                }
                preferredOpintooikeus.kaytossa = true
            }
        }
    }

    override fun updateMuokkausoikeudet(userId: String, muokkausoikeudet: Boolean) {
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(userId)
            ?.let {
                it.muokkausoikeudetVirkailijoilla = muokkausoikeudet
                opintooikeusRepository.save(it)
            }
    }

    override fun updateOpintooikeudet(
        userId: String,
        opintooikeudet: List<KayttajahallintaOpintooikeusDTO>
    ) {
        val oikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        oikeudet.forEach {
            opintooikeudet.firstOrNull { o -> o.id == it.id }?.let { o ->
                it.osaamisenArvioinninOppaanPvm = o.osaamisenArvioinninOppaanPvm
                opintoopasRepository.findByIdOrNull(o.opintoopas)
                    ?.let { opas -> it.opintoopas = opas }
            }
        }
        opintooikeusRepository.saveAll(oikeudet)
    }

    private fun getImpersonatedOpintooikeusId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal: Saml2AuthenticatedPrincipal =
            authentication.principal as Saml2AuthenticatedPrincipal
        val authorities = authentication.authorities.map { it.authority }
        if (authorities.contains(ERIKOISTUVA_LAAKARI_IMPERSONATED) || authorities.contains(
                ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
            )
        ) {
            return principal.getFirstAttribute("opintooikeusId") as Long
        }
        return null
    }
}
