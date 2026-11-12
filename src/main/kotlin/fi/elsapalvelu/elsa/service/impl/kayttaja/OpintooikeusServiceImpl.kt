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
import fi.elsapalvelu.elsa.security.ADMIN
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
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
import jakarta.validation.ValidationException

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

    override fun checkOpintooikeusAndRoles(user: User) {
        val currentUser = userRepository.findByIdWithAuthorities(user.id.required())
            .orElseThrow { EntityNotFoundException("Käyttäjää ei löydy") }
        val validOikeudet = opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId(
            currentUser.id.required()
        )

        updateRoles(validOikeudet, currentUser)
        checkOpintooikeusKaytossaValid(validOikeudet, currentUser)
        if (currentUser.activeAuthority == null && currentUser.authorities.isNotEmpty()) {
            currentUser.activeAuthority = currentUser.authorities.first()
            userRepository.save(currentUser)
        }
        // The caller may have supplied a detached snapshot; never merge its unrelated fields into the current User.
        if (user !== currentUser) {
            user.authorities = currentUser.authorities.toMutableSet()
            user.activeAuthority = currentUser.activeAuthority
        }
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

                // ELSAINSI-73: opinto-oikeuden vaihto (navigaatiopalkin pudotusvalikko) ei saa jättää
                // aktiivista roolia ristiriitaan käytössä olevan opinto-oikeuden kanssa
                // (esim. YEK-rooli + EL-opinto-oikeus käytössä -> YEK-rajapinnat: "Opinto-oikeutta ei löydy").
                userRepository.findByIdWithAuthorities(userId).ifPresent { user ->
                    alignActiveAuthorityWithOpintooikeus(user, preferredOpintooikeus)
                }
            }
        }
    }

    private fun alignActiveAuthorityWithOpintooikeus(user: User, opintooikeus: Opintooikeus) {
        val current = user.activeAuthority?.name
        // Muihin rooleihin (kouluttaja, vastuuhenkilö jne.) ei kosketa.
        if (current != ERIKOISTUVA_LAAKARI && current != YEK_KOULUTETTAVA) {
            return
        }
        val target =
            if (opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID) YEK_KOULUTETTAVA else ERIKOISTUVA_LAAKARI
        if (current == target) {
            return
        }
        user.authorities.firstOrNull { it.name == target }?.let {
            user.activeAuthority = it
            userRepository.save(user)
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

    // Legacy SAML principal compatibility; migrate with login, logout and impersonation.
    @Suppress("DEPRECATION", "Deprecation")
    private fun getImpersonatedOpintooikeusId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
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
        val previousAuthorities = user.authorities.toSet()
        val previousActiveAuthority = user.activeAuthority
        val yekOikeus = validOikeudet.map { it.erikoisala?.id }.contains(YEK_ERIKOISALA_ID)
        val elOikeus = validOikeudet.any { it.erikoisala?.id != YEK_ERIKOISALA_ID }
        val authorities = user.authorities

        // An existing study right can become valid again after an import extends it.
        // Restore its role without changing the user's active role or selected study right.
        if (elOikeus) {
            authorities.add(Authority(name = ERIKOISTUVA_LAAKARI))
        }
        if (yekOikeus) {
            authorities.add(Authority(name = YEK_KOULUTETTAVA))
        }

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

        if (user.authorities != previousAuthorities || user.activeAuthority != previousActiveAuthority) {
            userRepository.save(user)
        }
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
        val oikeudetKaytossa = opintooikeudet.singleOrNull { it.kaytossa }
        val validOikeusKaytossa = oikeudetKaytossa?.let { opintooikeus ->
            validOikeudet.any { it.id == opintooikeus.id }
        } == true

        if (oikeudetKaytossa != null && validOikeusKaytossa) {
            alignKaytossaWithActiveAuthority(opintooikeudet, validOikeudet, oikeudetKaytossa, user)
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

    /**
     * ELSAINSI-73: käytössä oleva opinto-oikeus voi olla voimassa, mutta ristiriidassa aktiivisen roolin kanssa
     * (YEK-rooli + EL-opinto-oikeus tai päinvastoin). Tällöin YEK-rajapinnat heittävät "Opinto-oikeutta ei löydy"
     * ja EL-rajapinnat käyttävät väärää opinto-oikeutta. Korjataan käytössä oleva opinto-oikeus vastaamaan roolia,
     * tai jos sopivaa opinto-oikeutta ei ole, rooli vastaamaan opinto-oikeutta.
     */
    private fun alignKaytossaWithActiveAuthority(
        opintooikeudet: List<Opintooikeus>,
        validOikeudet: List<Opintooikeus>,
        kaytossa: Opintooikeus,
        user: User
    ) {
        val activeAuthority = user.activeAuthority?.name
        val kaytossaYek = kaytossa.erikoisala?.id == YEK_ERIKOISALA_ID

        val replacement = when {
            activeAuthority == YEK_KOULUTETTAVA && !kaytossaYek ->
                validOikeudet.firstOrNull { it.erikoisala?.id == YEK_ERIKOISALA_ID }
            activeAuthority == ERIKOISTUVA_LAAKARI && kaytossaYek ->
                getPreferredOpintooikeus(validOikeudet.filter { it.erikoisala?.id != YEK_ERIKOISALA_ID })
            else -> return
        }

        if (replacement != null) {
            opintooikeudet.forEach { it.kaytossa = it.id == replacement.id }
            if (replacement.erikoisala?.id != YEK_ERIKOISALA_ID) {
                replacement.erikoistuvaLaakari?.let {
                    it.aktiivinenOpintooikeus = replacement.id
                    erikoistuvaLaakariRepository.save(it)
                }
            }
            return
        }

        alignActiveAuthorityWithOpintooikeus(user, kaytossa)
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
    // Säilytetään valittu henkilökunnan rooli myös tuonnin jälkeisessä täsmäytyksessä.
    val activeAuthority = user.activeAuthority
    val staffRoles = setOf(KOULUTTAJA, VASTUUHENKILO, OPINTOHALLINNON_VIRKAILIJA, TEKNINEN_PAAKAYTTAJA, ADMIN)
    if (activeAuthority?.name in staffRoles && activeAuthority in user.authorities) {
        return activeAuthority
    }
    val authorityNames = user.authorities.map { it.name }
    // Muuten oletusroolin prioriteetti on EL, vastuuhenkilö, kouluttaja ja YEK.
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
