package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.constants.ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
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

    @GetMapping("/kouluttajat")
    fun getKouluttajat(
        criteria: KayttajahallintaCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<KayttajahallintaKayttajaListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val kouluttajat = if (hasVirkailijaRole(user)) {
            kayttajaService.findByKayttajahallintaCriteriaFromSameYliopisto(
                user.id!!, KOULUTTAJA, criteria, pageable
            )
        } else {
            kayttajaService.findByKayttajahallintaCriteria(
                user.id!!, KOULUTTAJA, criteria, pageable
            )
        }
        return ResponseEntity.ok(kouluttajat)
    }

    @GetMapping("/virkailijat")
    fun getVirkailijat(
        criteria: KayttajahallintaCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<KayttajahallintaKayttajaListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val virkailijat = if (hasVirkailijaRole(user)) {
            kayttajaService.findByKayttajahallintaCriteriaFromSameYliopisto(
                user.id!!, OPINTOHALLINNON_VIRKAILIJA, criteria, pageable
            )
        } else {
            kayttajaService.findByKayttajahallintaCriteria(
                user.id!!, OPINTOHALLINNON_VIRKAILIJA, criteria, pageable
            )
        }
        return ResponseEntity.ok(virkailijat)
    }

    @GetMapping("/paakayttajat")
    fun getPaakayttajat(
        criteria: KayttajahallintaCriteria, pageable: Pageable, principal: Principal?
    ): ResponseEntity<Page<KayttajahallintaKayttajaListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)

        if (hasVirkailijaRole(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val paakayttajat = kayttajaService.findByKayttajahallintaCriteria(
            user.id!!, TEKNINEN_PAAKAYTTAJA, criteria, pageable
        )
        return ResponseEntity.ok(paakayttajat)
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
                kayttaja = getKayttajaOrThrow(id),
                erikoistuvaLaakari = erikoistuvaLaakari
            )
        )
    }

    @GetMapping("/kayttajat/{id}")
    fun getKayttaja(
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

    @GetMapping("/yliopistot")
    fun getYliopistotByRole(principal: Principal?): ResponseEntity<Set<YliopistoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(getYliopistotByRole(user))
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
        validateEmailNotExists(kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite!!)

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

        return ResponseEntity.created(URI("/api/${resolveRolePath(principal)}/erikoistuvat-laakarit/${result.kayttajaId}"))
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
        validateEmailNotExists(sahkoposti, userDTO)
        userService.updateEmail(sahkoposti, userId)

        return ResponseEntity.ok().build()
    }

    @PostMapping("/virkailijat")
    fun createVirkailija(
        @Valid @RequestBody kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaWrapperDTO> {
        val yliopistoId = kayttajahallintaKayttajaDTO.yliopisto?.id
        requireNotNull(yliopistoId)
        validateCurrentUserIsAllowedToCreateKayttajaByYliopistoId(
            principal, yliopistoId
        )

        val etunimi = kayttajahallintaKayttajaDTO.etunimi
        val sukunimi = kayttajahallintaKayttajaDTO.sukunimi
        val sahkoposti = kayttajahallintaKayttajaDTO.sahkoposti
        val eppn = kayttajahallintaKayttajaDTO.eppn

        requireNotNull(listOf(etunimi, sukunimi))

        validateEmailNotExists(sahkoposti)
        validateEppnNotExists(eppn)

        val result = kayttajaService.saveKayttajahallintaKayttaja(
            kayttajahallintaKayttajaDTO, setOf(
                OPINTOHALLINNON_VIRKAILIJA
            )
        )
        return ResponseEntity.created(URI("/api/${resolveRolePath(principal)}/vastuuhenkilot/${result.kayttaja?.id}"))
            .body(result)
    }

    @PostMapping("/paakayttajat")
    fun createPaakayttaja(
        @Valid @RequestBody kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        principal: Principal?
    ): ResponseEntity<KayttajahallintaKayttajaWrapperDTO> {
        val user = userService.getAuthenticatedUser(principal)
        if (hasVirkailijaRole(user)) {
            throw getVirkailijaException()
        }

        val etunimi = kayttajahallintaKayttajaDTO.etunimi
        val sukunimi = kayttajahallintaKayttajaDTO.sukunimi
        val sahkoposti = kayttajahallintaKayttajaDTO.sahkoposti
        val eppn = kayttajahallintaKayttajaDTO.eppn

        requireNotNull(listOf(etunimi, sukunimi))

        validateEmailNotExists(sahkoposti)
        validateEppnNotExists(eppn)

        val result = kayttajaService.saveKayttajahallintaKayttaja(
            kayttajahallintaKayttajaDTO, setOf(
                TEKNINEN_PAAKAYTTAJA
            )
        )
        return ResponseEntity.created(URI("/api/tekninen-paakayttaja/paakayttajat/${result.kayttaja?.id}"))
            .body(result)
    }

    @PatchMapping("/paakayttajat/{kayttajaId}")
    fun patchPaakayttaja(
        @PathVariable kayttajaId: Long,
        @Valid @RequestBody kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        principal: Principal?
    ): ResponseEntity<Void> {
        val existingKayttajaDTO = getKayttajaOrThrow(kayttajaId)
        validateCurrentUserIsAllowedToManageKayttaja(principal, existingKayttajaDTO.id!!)

        val sahkoposti = kayttajahallintaKayttajaDTO.sahkoposti
        val eppn = kayttajahallintaKayttajaDTO.eppn
        val userDTO = userService.getUser(existingKayttajaDTO.userId!!)

        validateEmailNotExists(sahkoposti, userDTO)
        validateEppnNotExists(eppn, userDTO)

        kayttajaService.saveKayttajahallintaKayttaja(kayttajahallintaKayttajaDTO, kayttajaId = kayttajaId)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/virkailijat/{kayttajaId}")
    fun patchVirkailija(
        @PathVariable kayttajaId: Long,
        @Valid @RequestBody kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        principal: Principal?
    ): ResponseEntity<Void> {
        val existingKayttajaDTO = getKayttajaOrThrow(kayttajaId)
        validateCurrentUserIsAllowedToManageKayttaja(principal, existingKayttajaDTO.id!!)

        val sahkoposti = kayttajahallintaKayttajaDTO.sahkoposti
        val eppn = kayttajahallintaKayttajaDTO.eppn
        val userDTO = userService.getUser(existingKayttajaDTO.userId!!)

        validateEmailNotExists(sahkoposti, userDTO)
        validateEppnNotExists(eppn, userDTO)

        kayttajaService.saveKayttajahallintaKayttaja(kayttajahallintaKayttajaDTO, kayttajaId = kayttajaId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/vastuuhenkilot")
    fun createVastuuhenkilo(
        @Valid @RequestBody kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        principal: Principal?
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

        validateEmailNotExists(sahkoposti)
        validateEppnNotExists(eppn)

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

        return ResponseEntity.created(URI("/api/${resolveRolePath(principal)}/vastuuhenkilot/${result.kayttaja?.id}"))
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

        validateEmailNotExists(sahkoposti, userDTO)
        validateEppnNotExists(eppn, userDTO)

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

    private fun validateEppnNotExists(eppn: String, userDTO: UserDTO? = null) {
        if ((userDTO == null || userDTO.eppn != eppn) && userService.existsByEppn(eppn)) {
            throw BadRequestAlertException(
                "Samalla yliopiston käyttäjätunnuksella löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-eppn-tunnuksella-loytyy-jo-toinen-kayttaja"
            )
        }
    }

    private fun validateEmailNotExists(sahkoposti: String, userDTO: UserDTO? = null) {
        if ((userDTO == null || userDTO.email != sahkoposti) && userService.existsByEmail(sahkoposti)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                KAYTTAJA_ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }
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

    private fun resolveRolePath(principal: Principal?): String {
        val virkailijaOrPaakayttajaUser = userService.getAuthenticatedUser(principal)
        return if (hasVirkailijaRole(virkailijaOrPaakayttajaUser)) "virkailija" else "tekninen-paakayttaja"
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
                ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR,
                ERIKOISTUVA_LAAKARI_ENTITY_NAME,
                "dataillegal.erikoistuvaa-laakaria-ei-loydy"
            )
        }

    private fun getErikoistuvaLaakariByKayttajaIdOrThrow(kayttajaId: Long): ErikoistuvaLaakariDTO =
        erikoistuvaLaakariService.findOneByKayttajaId(kayttajaId) ?: throw
        BadRequestAlertException(
            ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR,
            ERIKOISTUVA_LAAKARI_ENTITY_NAME,
            "dataillegal.erikoistuvaa-laakaria-ei-loydy"
        )

    private fun getErikoistuvaLaakariByUserIdOrThrow(userId: String): ErikoistuvaLaakariDTO =
        erikoistuvaLaakariService.findOneByKayttajaUserId(userId) ?: throw
        BadRequestAlertException(
            ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR,
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
