package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.required

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintoopasRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.constants.OPINTOOIKEUS_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.dto.kayttaja.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaOpintooikeusDTO
import fi.elsapalvelu.elsa.service.mapper.kayttaja.OpintooikeusMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import javax.xml.bind.ValidationException

@Service
@Transactional
class OpintooikeusServiceImpl(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintooikeusMapper: OpintooikeusMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val clock: Clock,
    private val opintoopasRepository: OpintoopasRepository,
    private val userRepository: UserRepository
) : OpintooikeusService {
    override fun findAllValidByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO> {
        return opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
            userId
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
                return it.id.required()
            }

        throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    override fun findOneByErikoisalaIdAndErikoistuvaLaakariKayttajaUserId(erikoisalaId: Long, userId: String): Long {
        getImpersonatedOpintooikeusId()?.let { return it }
        opintooikeusRepository.findOneByErikoisalaIdAndErikoistuvaLaakariKayttajaUserId(erikoisalaId, userId)
            ?.let {
                return it.id.required()
            }

        throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    override fun findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
        userId: String, erikoisalaId: Long
    ): Long {
        getImpersonatedOpintooikeusId()?.let { return it }
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrueAndErikoisalaId(userId, erikoisalaId)
            ?.let {
                return it.id.required()
            }

        throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    override fun findOneByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
        userId: String, erikoisalaId: Long
    ): OpintooikeusDTO {
        getImpersonatedOpintooikeusId()?.let {
            return opintooikeusMapper.toDto(
                opintooikeusRepository.findById(it).get()
            )
        }
        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrueAndErikoisalaId(userId, erikoisalaId)
            ?.let {
                return opintooikeusMapper.toDto(it)
            }

        throw EntityNotFoundException(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    override fun findAllByTerveyskoulutusjaksoSuorittamatta(): List<Opintooikeus> {
        return opintooikeusRepository.findAllByTerveyskoulutusjaksoSuorittamatta()
    }

    override fun onOikeus(user: User): Boolean {
        if (opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
                user.id.required()
            ).any()
        ) {
            return true
        }

        return false
    }

    override fun checkOpintooikeusAndRoles(user: User) {
        // Tarkistetaan, että käyttäjällä on aktiivinen rooli (muuten kaikkia käyttäjätietoja ei välttämättä pystytä noutamaa)
        // ja lisätään väliaikainen ROLE_USER-rooli, mikäli roolia ei ole
        if (user.activeAuthority == null) {
            val authority = user.authorities.firstOrNull() ?: Authority("ROLE_USER")
            userRepository.setActiveAuthorityIfNull(user.id.required(), authority)
        }
        val validOikeudet = opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
            user.id.required()
        )

        updateRoles(validOikeudet, user)
        checkOpintooikeusKaytossaValid(validOikeudet, user)
    }

    override fun reconcileOpintooikeusKaytossaAfterImport(userId: String) {
        val opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        val validOikeudet = opintooikeudet.filter { it.isValidForUse(clock) }
        val user = userRepository.findByIdWithAuthorities(userId)
            .orElseThrow { EntityNotFoundException("Käyttäjää ei löydy") }
        reconcileOpintooikeusKaytossa(opintooikeudet, validOikeudet, user)
    }

    override fun setOpintooikeusKaytossa(userId: String, opintooikeusId: Long) {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuva ->
            opintooikeusRepository.findOneByIdAndErikoistuvaLaakariIdAndBetweenDate(
                opintooikeusId, erikoistuva.id.required(), LocalDate.now(clock)
            )?.let { preferredOpintooikeus ->
                erikoistuva.opintooikeudet.forEach { opintooikeus ->
                    opintooikeus.kaytossa = false
                }
                preferredOpintooikeus.kaytossa = true

                if (preferredOpintooikeus.erikoisala?.id != YEK_ERIKOISALA_ID) {
                    erikoistuva.aktiivinenOpintooikeus = preferredOpintooikeus.id
                }
            }
        }
    }

    override fun setAktiivinenOpintooikeusKaytossa(userId: String) {
        val validOikeudet = opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
            userId)
            .filter { it.erikoisala?.id != YEK_ERIKOISALA_ID }

        if (validOikeudet.isEmpty()) {
            throw ValidationException("Käyttäjällä ei ole voimassaolevaa opinto-oikeutta")
        }

        val aktiivinenOikeus = validOikeudet.firstOrNull { it.id == it.erikoistuvaLaakari?.aktiivinenOpintooikeus }

        val opintoOikeudet = opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
        opintoOikeudet.forEach { it.kaytossa = false }

        if (aktiivinenOikeus == null) {
            validOikeudet.first().kaytossa = true
        } else {
            aktiivinenOikeus.kaytossa = true
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
                o.opintoopas?.let { opintoopasRepository.findByIdOrNull(it) }
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

    private fun updateRoles(validOikeudet: List<Opintooikeus>, user: User) {
        val yekOikeus = validOikeudet.map { it.erikoisala?.id }.contains(YEK_ERIKOISALA_ID)
        val elOikeus = validOikeudet.any { it.erikoisala?.id != YEK_ERIKOISALA_ID }
        val authorities = user.authorities

        val elAuthority = authorities.find { it.name == ERIKOISTUVA_LAAKARI }
        if (!elOikeus && elAuthority != null) {
            authorities.remove(elAuthority)

            if (user.activeAuthority == elAuthority) {
                user.activeAuthority = authorities.firstOrNull()
            }
        }

        val yekAuthority = authorities.find { it.name == YEK_KOULUTETTAVA }
        if (!yekOikeus && yekAuthority != null) {
            authorities.remove(yekAuthority)

            if (user.activeAuthority == yekAuthority) {
                user.activeAuthority = authorities.firstOrNull()
            }
        }

        val elOikeudet = validOikeudet.filter { it.erikoisala?.id != YEK_ERIKOISALA_ID }

        erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id.required())?.let { erikoistuvaLaakari ->
            if (elOikeus && elOikeudet.none { it.id == erikoistuvaLaakari.aktiivinenOpintooikeus }) {
                erikoistuvaLaakari.aktiivinenOpintooikeus = elOikeudet.first().id
                erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
            }
        }

        userRepository.save(user)
    }

    private fun checkOpintooikeusKaytossaValid(validOikeudet: List<Opintooikeus>, user: User) {
        val opintooikeudet =
            opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(user.id.required())
        reconcileOpintooikeusKaytossa(
            opintooikeudet,
            validOikeudet.filter { it.tila != OpintooikeudenTila.VANHENTUNUT },
            user
        )
    }

    private fun reconcileOpintooikeusKaytossa(
        opintooikeudet: List<Opintooikeus>,
        validOikeudet: List<Opintooikeus>,
        user: User
    ) {
        val oikeudetKaytossa = opintooikeudet.filter { it.kaytossa }
        val validOikeusKaytossa = oikeudetKaytossa.singleOrNull()?.let { opintooikeus ->
            validOikeudet.any { it.id == opintooikeus.id }
        } == true

        if (validOikeusKaytossa) {
            return
        }

        val replacement = getPreferredOpintooikeus(validOikeudet) ?: return
        opintooikeudet.forEach { it.kaytossa = it.id == replacement.id }

        getPreferredAuthority(user, validOikeudet)?.let {
            user.activeAuthority = it
        }
        userRepository.save(user)

        if (replacement.erikoisala?.id != YEK_ERIKOISALA_ID) {
            replacement.erikoistuvaLaakari?.let {
                it.aktiivinenOpintooikeus = replacement.id
                erikoistuvaLaakariRepository.save(it)
            }
        }
    }
}

private fun getPreferredOpintooikeus(validOikeudet: List<Opintooikeus>): Opintooikeus? {
    val elOikeudet = validOikeudet.filter { it.erikoisala?.id != YEK_ERIKOISALA_ID }
    val aktiivinenOpintooikeus = elOikeudet.firstOrNull()?.erikoistuvaLaakari?.aktiivinenOpintooikeus
    // Säilytetään käyttäjän aiempi aktiivinen EL-opinto-oikeus, jos se on edelleen voimassa.
    return elOikeudet.firstOrNull { it.id == aktiivinenOpintooikeus }
        ?: elOikeudet.firstOrNull()
        ?: validOikeudet.firstOrNull()
}

private fun getPreferredAuthority(user: User, validOikeudet: List<Opintooikeus>): Authority? {
    val authorityNames = user.authorities.map { it.name }
    // Oletusroolin prioriteetti on EL, vastuuhenkilö, kouluttaja ja YEK. Muita rooleja ei poisteta.
    val preferredAuthorityName = when {
        validOikeudet.any { it.erikoisala?.id != YEK_ERIKOISALA_ID } &&
            authorityNames.contains(ERIKOISTUVA_LAAKARI) -> ERIKOISTUVA_LAAKARI
        authorityNames.contains(VASTUUHENKILO) -> VASTUUHENKILO
        authorityNames.contains(KOULUTTAJA) -> KOULUTTAJA
        validOikeudet.any { it.erikoisala?.id == YEK_ERIKOISALA_ID } &&
            authorityNames.contains(YEK_KOULUTETTAVA) -> YEK_KOULUTETTAVA
        else -> null
    }
    return user.authorities.firstOrNull { it.name == preferredAuthorityName }
}

private fun Opintooikeus.isValidForUse(clock: Clock): Boolean {
    val currentDate = LocalDate.now(clock)
    return opintooikeudenMyontamispaiva?.let { !currentDate.isBefore(it) } == true &&
        viimeinenKatselupaiva?.let { !currentDate.isAfter(it) } == true &&
        tila != OpintooikeudenTila.VANHENTUNUT &&
        erikoisala?.liittynytElsaan == true
}
