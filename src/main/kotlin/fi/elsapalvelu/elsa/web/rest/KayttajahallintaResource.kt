package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.constants.erikoistuvaLaakariNotFoundError
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val KAYTTAJA_ENTITY_NAME = "kayttaja"
private const val ERIKOISTUVA_LAAKARI_ENTITY_NAME = "erikoistuvaLaakari"

open class KayttajahallintaResource(
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val yliopistoService: YliopistoService,
    private val erikoisalaService: ErikoisalaService,
    private val asetusService: AsetusService,
    private val opintoopasService: OpintoopasService,
    private val kayttajahallintaValidationService: KayttajahallintaValidationService,
    private val mailService: MailService
) {
    @GetMapping("/erikoistuvat-laakarit")
    fun getErikoistuvatLaakarit(
        criteria: KayttajahallintaCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<KayttajahallintaKayttajaListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val erikoistujat = if (hasVirkailijaRole(user)) {
            erikoistuvaLaakariService.findAllFromSameYliopisto(user.id!!, criteria, pageable)
        } else {
            erikoistuvaLaakariService.findAll(user.id!!, criteria, pageable)
        }
        return ResponseEntity.ok(erikoistujat)
    }

    @GetMapping("/vastuuhenkilot")
    fun getVastuuhenkilot(
        criteria: KayttajahallintaCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<KayttajahallintaKayttajaListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val vastuuhenkilot = if (hasVirkailijaRole(user)) {
            kayttajaService.findByKayttajahallintaCriteriaFromSameYliopisto(
                user.id!!, VASTUUHENKILO, criteria, pageable
            )
        } else {
            kayttajaService.findByKayttajahallintaCriteria(
                user.id!!, VASTUUHENKILO, criteria, pageable
            )
        }
        return ResponseEntity.ok(vastuuhenkilot)
    }

    @GetMapping("/kayttajat/rajaimet")
    fun getKayttajahallintaRajaimet(): ResponseEntity<KayttajahallintaKayttajatOptionsDTO> {
        val form = KayttajahallintaKayttajatOptionsDTO()
        form.erikoisalat = erikoisalaService.findAllByLiittynytElsaan().toSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/erikoistuvat-laakarit/{id}")
    fun getErikoistuvaLaakari(
        @PathVariable id: Long, principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaWrapperDTO> {
        val erikoistuvaLaakari = getErikoistuvaLaakariByKayttajaIdOrThrow(id)
        validateCurrentUserIsAllowedToManageErikoistuvaLaakari(principal, id)

        return ResponseEntity.ok(
            KayttajahallintaKayttajaWrapperDTO(
                kayttaja = getKayttajaOrThrow(id), erikoistuvaLaakari = erikoistuvaLaakari
            )
        )
    }

    @GetMapping("/vastuuhenkilot/{id}")
    fun getVastuuhenkilo(
        @PathVariable id: Long, principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaWrapperDTO> {
        val kayttaja = getKayttajaOrThrow(id)
        validateCurrentUserIsAllowedToManageKayttaja(principal, id)

        return ResponseEntity.ok(
            KayttajahallintaKayttajaWrapperDTO(
                kayttaja = kayttaja
            )
        )
    }

    @GetMapping("/erikoistuva-laakari-lomake")
    fun getErikoistujaForm(principal: Principal?): ResponseEntity<KayttajahallintaErikoistuvaLaakariFormDTO> {
        val form = KayttajahallintaErikoistuvaLaakariFormDTO()
        val user = userService.getAuthenticatedUser(principal)

        form.yliopistot = getYliopistotByRole(user)
        form.erikoisalat = erikoisalaService.findAllByLiittynytElsaan().toMutableSet()
        form.asetukset = asetusService.findAll().toMutableSet()
        form.opintooppaat = opintoopasService.findAll().toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/vastuuhenkilo-lomake")
    fun getVastuuhenkiloForm(principal: Principal?): ResponseEntity<KayttajahallintaKayttajaFormDTO> {
        val form = KayttajahallintaKayttajaFormDTO()
        val user = userService.getAuthenticatedUser(principal)

        form.yliopistot = getYliopistotByRole(user)

        return ResponseEntity.ok(form)
    }

    @GetMapping("/vastuuhenkilon-tehtavat-lomake/{yliopistoId}")
    fun getVastuuhenkilonTehtavatForm(
        @PathVariable yliopistoId: Long
    ): ResponseEntity<KayttajahallintaVastuuhenkilonTehtavatFormDTO> {
        val form = KayttajahallintaVastuuhenkilonTehtavatFormDTO(
            erikoisalat = erikoisalaService.findAllByLiittynytElsaanWithTehtavatyypit().toSet(),
            vastuuhenkilot = kayttajaService.findVastuuhenkilotByYliopisto(yliopistoId).map {
                KayttajahallintaFormVastuuhenkiloDTO(
                    id = it.id,
                    etunimi = it.etunimi,
                    sukunimi = it.sukunimi,
                    yliopistotAndErikoisalat = it.yliopistotAndErikoisalat
                )
            }.toSet()
        )
        return ResponseEntity.ok(form)
    }

    @PostMapping("/erikoistuvat-laakarit")
    fun createErikoistuvaLaakari(
        @Valid @RequestBody kayttajahallintaErikoistuvaLaakariDTO: KayttajahallintaErikoistuvaLaakariDTO,
        principal: Principal?
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        if (userService.existsByEmail(kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite!!)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }

        if (yliopistoService.findOne(kayttajahallintaErikoistuvaLaakariDTO.yliopistoId!!).isEmpty) {
            throw BadRequestAlertException(
                "Yliopistoa ei löydy.", KAYTTAJA_ENTITY_NAME, "dataillegal.yliopistoa-ei-loydy"
            )
        }

        validateCurrentUserIsAllowedToCreateKayttajaByYliopistoId(
            principal, kayttajahallintaErikoistuvaLaakariDTO.yliopistoId!!
        )

        if (erikoisalaService.findOne(kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId!!).isEmpty) {
            throw BadRequestAlertException(
                "Erikoisalaa ei löydy.", KAYTTAJA_ENTITY_NAME, "dataillegal.erikoisalaa-ei-loydy"
            )
        }

        if (asetusService.findOne(kayttajahallintaErikoistuvaLaakariDTO.asetusId!!) == null) {
            throw BadRequestAlertException(
                "Asetusta ei löydy.", KAYTTAJA_ENTITY_NAME, "dataillegal.asetusta-ei-loydy"
            )
        }

        if (opintoopasService.findOne(kayttajahallintaErikoistuvaLaakariDTO.opintoopasId!!) == null) {
            throw BadRequestAlertException(
                "Opinto-opasta ei löydy.", KAYTTAJA_ENTITY_NAME, "dataillegal.opinto-opasta-ei-loydy"
            )
        }

        val result = erikoistuvaLaakariService.save(kayttajahallintaErikoistuvaLaakariDTO)

        return ResponseEntity.created(URI("/api/tekninen-paakayttaja/erikoistuvat-laakarit/${result.kayttajaId}"))
            .body(result)

    }

    @PutMapping("/erikoistuvat-laakarit/{id}/kutsu")
    fun resendErikoistuvaLaakariInvitation(
        @PathVariable id: Long, principal: Principal?
    ): ResponseEntity<Void> {
        val erikoistuvaLaakari = getErikoistuvaLaakariByIdOrThrow(id)
        validateCurrentUserIsAllowedToManageErikoistuvaLaakari(principal, erikoistuvaLaakari.kayttajaId!!)
        erikoistuvaLaakariService.resendInvitation(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/kayttajat/{id}/aktivoi")
    fun activateKayttaja(
        @PathVariable id: Long, principal: Principal?
    ): ResponseEntity<Void> {
        val erikoistuvaLaakari = tryToGetErikoistuvaLaakariByKayttajaId(id)
        if (erikoistuvaLaakari != null) {
            validateCurrentUserIsAllowedToManageErikoistuvaLaakari(principal, erikoistuvaLaakari.kayttajaId!!)
        } else {
            val kayttaja = getKayttajaOrThrow(id)
            validateCurrentUserIsAllowedToManageKayttaja(principal, kayttaja.id!!)
        }
        kayttajaService.activateKayttaja(id)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/kayttajat/{id}/passivoi")
    fun passivateKayttaja(
        @PathVariable id: Long, principal: Principal?
    ): ResponseEntity<Void> {
        val erikoistuvaLaakari = tryToGetErikoistuvaLaakariByKayttajaId(id)
        if (erikoistuvaLaakari != null) {
            validateCurrentUserIsAllowedToManageErikoistuvaLaakari(principal, erikoistuvaLaakari.id!!)
        } else {
            val kayttaja = getKayttajaOrThrow(id)
            validateCurrentUserIsAllowedToManageKayttaja(principal, kayttaja.id!!)
        }
        kayttajaService.passivateKayttaja(id)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/erikoistuvat-laakarit/{userId}")
    fun patchErikoistuvaLaakari(
        @PathVariable userId: String,
        @Valid @RequestBody updateErikoistuvaLaakariDTO: KayttajahallintaErikoistuvaLaakariUpdateDTO,
        principal: Principal?
    ): ResponseEntity<Void> {
        val erikoistuvaLaakariDTO = getErikoistuvaLaakariByUserIdOrThrow(userId)
        validateCurrentUserIsAllowedToManageErikoistuvaLaakari(principal, erikoistuvaLaakariDTO.kayttajaId!!)
        val sahkoposti = updateErikoistuvaLaakariDTO.sahkoposti

        val userDTO = userService.getUser(userId)
        if (userDTO.email != sahkoposti && userService.existsByEmail(sahkoposti)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }
        userService.updateEmail(sahkoposti, userId)

        return ResponseEntity.ok().build()
    }

    @PostMapping("/vastuuhenkilot")
    fun createVastuuhenkilo(
        @Valid @RequestBody kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO, principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaWrapperDTO> {
        val yliopistoId = kayttajahallintaKayttajaDTO.yliopisto?.id
        requireNotNull(yliopistoId)
        validateDTOYliopistotAndErikoisalat(kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat)
        validateCurrentUserIsAllowedToCreateKayttajaByYliopistoId(
            principal, yliopistoId
        )

        val etunimi = kayttajahallintaKayttajaDTO.etunimi
        val sukunimi = kayttajahallintaKayttajaDTO.sukunimi
        val sahkoposti = kayttajahallintaKayttajaDTO.sahkoposti
        val eppn = kayttajahallintaKayttajaDTO.eppn

        requireNotNull(listOf(etunimi, sukunimi))

        if (userService.existsByEmail(sahkoposti)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }

        if (userService.existsByEppn(eppn)) {
            throw BadRequestAlertException(
                "Samalla yliopiston käyttäjätunnuksella löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-eppn-tunnuksella-loytyy-jo-toinen-kayttaja"
            )
        }

        if (!kayttajahallintaValidationService.validateNewVastuuhenkiloYliopistotAndErikoisalat(
                kayttajahallintaKayttajaDTO
            )
        ) throw getVastuuhenkilonTehtavatException()

        val result = kayttajaService.saveVastuuhenkilo(kayttajahallintaKayttajaDTO)

        mailService.sendEmailFromTemplate(
            User(email = kayttajahallintaKayttajaDTO.sahkoposti),
            templateName = "uusiVastuuhenkilo.html",
            titleKey = "email.uusiVastuuhenkilo.title",
            properties = mapOf()
        )

        return ResponseEntity.created(URI("/api/tekninen-paakayttaja/vastuuhenkilot/${result.kayttaja?.id}"))
            .body(result)
    }

    @PutMapping("/vastuuhenkilot/{kayttajaId}")
    fun putVastuuhenkilo(
        @PathVariable kayttajaId: Long,
        @Valid @RequestBody kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaWrapperDTO> {
        val existingKayttajaDTO = getKayttajaOrThrow(kayttajaId)
        val yliopistoId = existingKayttajaDTO.yliopistotAndErikoisalat?.firstOrNull()?.yliopisto?.id
        val givenYliopistotAndErikoisalat = kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat

        validateYliopisto(givenYliopistotAndErikoisalat, yliopistoId)
        validateDTOYliopistotAndErikoisalat(
            givenYliopistotAndErikoisalat,
            existingKayttajaDTO.yliopistotAndErikoisalat?.firstOrNull()?.yliopisto?.id
        )
        validateCurrentUserIsAllowedToManageKayttaja(principal, kayttajaId)

        val sahkoposti = kayttajahallintaKayttajaDTO.sahkoposti
        val eppn = kayttajahallintaKayttajaDTO.eppn

        val userDTO = userService.getUser(existingKayttajaDTO.userId!!)
        if (userDTO.email != sahkoposti && userService.existsByEmail(sahkoposti)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }

        if (userDTO.eppn != eppn && userService.existsByEppn(eppn)) {
            throw BadRequestAlertException(
                "Samalla yliopiston käyttäjätunnuksella löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-eppn-tunnuksella-loytyy-jo-toinen-kayttaja"
            )
        }

        if (!kayttajahallintaValidationService.validateExistingVastuuhenkiloYliopistotAndErikoisalat(
                kayttajahallintaKayttajaDTO,
                existingKayttajaDTO
            )
        ) throw getVastuuhenkilonTehtavatException()

        val result = kayttajaService.saveVastuuhenkilo(kayttajahallintaKayttajaDTO, kayttajaId)
        return ResponseEntity.ok(result)
    }

    private fun getYliopistotByRole(user: UserDTO): MutableSet<YliopistoDTO> {
        if (hasVirkailijaRole(user)) {
            val kayttaja = getKayttajaByUserIdOrThrow(user.id!!)
            val virkailijaYliopisto = getVirkailijaYliopistoOrThrow(kayttaja)
            return mutableSetOf(virkailijaYliopisto)
        }

        return yliopistoService.findAll().toMutableSet()
    }

    private fun validateCurrentUserIsAllowedToCreateKayttajaByYliopistoId(
        principal: Principal?, yliopistoId: Long
    ) {
        val virkailijaOrPaakayttajaUser = userService.getAuthenticatedUser(principal)
        if (hasVirkailijaRole(virkailijaOrPaakayttajaUser)) {
            if (!kayttajahallintaValidationService.validateVirkailijaIsAllowedToCreateKayttajaByYliopistoId(
                    virkailijaOrPaakayttajaUser, yliopistoId
                )
            ) {
                throw getVirkailijaException()
            }
        }
    }

    private fun validateCurrentUserIsAllowedToManageErikoistuvaLaakari(
        principal: Principal?, kayttajaId: Long
    ) {
        val virkailijaOrPaakayttajaUser = userService.getAuthenticatedUser(principal)
        if (hasVirkailijaRole(virkailijaOrPaakayttajaUser)) {
            if (!kayttajahallintaValidationService.validateVirkailijaIsAllowedToManageErikoistuvaLaakari(
                    virkailijaOrPaakayttajaUser, kayttajaId
                )
            ) {
                throw getVirkailijaException()
            }
        }
    }

    private fun validateCurrentUserIsAllowedToManageKayttaja(principal: Principal?, kayttajaId: Long) {
        val virkailijaOrPaakayttajaUser = userService.getAuthenticatedUser(principal)
        if (hasVirkailijaRole(virkailijaOrPaakayttajaUser)) {
            if (!kayttajahallintaValidationService.validateVirkailijaIsAllowedToManageKayttaja(
                    virkailijaOrPaakayttajaUser, kayttajaId
                )
            ) {
                throw getVirkailijaException()
            }
        }
    }

    private fun validateDTOYliopistotAndErikoisalat(
        yliopistotAndErikoisalat: Set<KayttajaYliopistoErikoisalaDTO>?,
        yliopistoId: Long? = null
    ) {
        requireNotNull(yliopistotAndErikoisalat)
        require(yliopistotAndErikoisalat.isNotEmpty())
        yliopistotAndErikoisalat.forEach { kayttajaYliopistoErikoisalaDTO ->
            yliopistoId?.let { require(kayttajaYliopistoErikoisalaDTO.yliopisto?.id == it) }
        }
    }

    private fun validateYliopisto(
        yliopistotAndErikoisalat: Set<KayttajaYliopistoErikoisalaDTO>?,
        yliopistoId: Long?
    ) {
        yliopistotAndErikoisalat?.forEach {
            if (it.yliopisto?.id != yliopistoId) {
                throw BadRequestAlertException(
                    "Vastuuhenkilön yliopistoa ei voi vaihtaa",
                    KAYTTAJA_ENTITY_NAME,
                    "dataillegal.vastuuhenkilon-yliopistoa-ei-voi-vaihtaa"
                )
            }
        }
    }

    private fun requireNotNull(values: List<String?>) {
        values.forEach { requireNotNull(it) }
    }

    private fun tryToGetErikoistuvaLaakariByKayttajaId(kayttajaId: Long): ErikoistuvaLaakariDTO? =
        erikoistuvaLaakariService.findOneByKayttajaId(kayttajaId)

    private fun hasVirkailijaRole(user: UserDTO): Boolean {
        return user.authorities?.contains(OPINTOHALLINNON_VIRKAILIJA) == true
    }

    private fun getVirkailijaYliopistoOrThrow(virkailijaDTO: KayttajaDTO) =
        virkailijaDTO.yliopistot?.firstOrNull() ?: throw BadRequestAlertException(
            "Virkailijalle ei ole määritetty yliopistoa",
            KAYTTAJA_ENTITY_NAME,
            "dataillegal.virkailijalle-ei-ole-maaritetty-yliopistoa"

        )

    private fun getKayttajaOrThrow(kayttajaId: Long): KayttajaDTO = kayttajaService.findOne(kayttajaId).orElseThrow {
        BadRequestAlertException(
            "Käyttäjää ei löydy", KAYTTAJA_ENTITY_NAME, "dataillegal.kayttajaa-ei-loydy"
        )
    }

    private fun getKayttajaByUserIdOrThrow(userId: String): KayttajaDTO =
        kayttajaService.findByUserId(userId).orElseThrow {
            BadRequestAlertException(
                "Käyttäjää ei löydy", KAYTTAJA_ENTITY_NAME, "dataillegal.kayttajaa-ei-loydy"
            )
        }

    private fun getErikoistuvaLaakariByIdOrThrow(id: Long): ErikoistuvaLaakariDTO =
        erikoistuvaLaakariService.findOne(id).orElseThrow {
            BadRequestAlertException(
                erikoistuvaLaakariNotFoundError,
                ERIKOISTUVA_LAAKARI_ENTITY_NAME,
                "dataillegal.erikoistuvaa-laakaria-ei-loydy"
            )
        }

    private fun getErikoistuvaLaakariByKayttajaIdOrThrow(kayttajaId: Long): ErikoistuvaLaakariDTO =
        erikoistuvaLaakariService.findOneByKayttajaId(kayttajaId) ?: throw
        BadRequestAlertException(
            erikoistuvaLaakariNotFoundError,
            ERIKOISTUVA_LAAKARI_ENTITY_NAME,
            "dataillegal.erikoistuvaa-laakaria-ei-loydy"
        )

    private fun getErikoistuvaLaakariByUserIdOrThrow(userId: String): ErikoistuvaLaakariDTO =
        erikoistuvaLaakariService.findOneByKayttajaUserId(userId) ?: throw
        BadRequestAlertException(
            erikoistuvaLaakariNotFoundError,
            ERIKOISTUVA_LAAKARI_ENTITY_NAME,
            "dataillegal.erikoistuvaa-laakaria-ei-loydy"
        )

    private fun getVirkailijaException() = BadRequestAlertException(
        "Virkailija voi hallinnoida vain oman yliopistonsa käyttäjiä",
        KAYTTAJA_ENTITY_NAME,
        "dataillegal.virkailija-voi-hallinnoida-vain-oman-yliopistonsa-kayttajia"
    )

    private fun getVastuuhenkilonTehtavatException() = BadRequestAlertException(
        "Virhe vastuuhenkilön tehtävien määrittelyssä",
        KAYTTAJA_ENTITY_NAME,
        "dataillegal.vastuuhenkilon-tehtavat-maaritettava-enintaan-yhdelle-vastuuhenkilolle"
    )
}
