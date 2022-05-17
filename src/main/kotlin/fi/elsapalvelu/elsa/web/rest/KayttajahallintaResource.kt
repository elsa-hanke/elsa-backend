package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KayttajahallintaKayttajatOptionsDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "kayttaja"

open class KayttajahallintaResource(
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val yliopistoService: YliopistoService,
    private val erikoisalaService: ErikoisalaService,
    private val asetusService: AsetusService,
    private val opintoopasService: OpintoopasService,
    private val vastuuhenkilonTehtavatyyppiService: VastuuhenkilonTehtavatyyppiService
) {
    @GetMapping("/erikoistuvat-laakarit")
    fun getErikoistuvatLaakarit(
        criteria: KayttajahallintaCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<KayttajahallintaKayttajaListItemDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val erikoistujat =
            if (hasVirkailijaRole(user)) {
                erikoistuvaLaakariService.findAllForVirkailija(user.id!!, criteria, pageable)
            } else {
                erikoistuvaLaakariService.findAll(user.id!!, criteria, pageable)
            }
        return ResponseEntity.ok(erikoistujat)
    }

    @GetMapping("/kayttajat/rajaimet")
    fun getKayttajahallintaRajaimet(): ResponseEntity<KayttajahallintaKayttajatOptionsDTO> {
        val form = KayttajahallintaKayttajatOptionsDTO()
        form.erikoisalat = erikoisalaService.findAllByLiittynytElsaan().toSet()
        form.vastuuhenkilonVastuualueet = vastuuhenkilonTehtavatyyppiService.findAll().toSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/kayttajat/{id}")
    fun getKayttaja(
        @PathVariable id: Long
    ): ResponseEntity<KayttajahallintaKayttajaDTO> {
        val kayttaja: KayttajaDTO? = kayttajaService.findOne(id).orElse(null)
        val user = kayttaja?.let { userService.getUser(it.userId!!) }
        val erikoistuvaLaakari = erikoistuvaLaakariService.findOneByKayttajaId(id)

        return ResponseEntity.ok(
            KayttajahallintaKayttajaDTO(
                user = user,
                kayttaja = kayttaja,
                erikoistuvaLaakari = erikoistuvaLaakari
            )
        )
    }

    @GetMapping("/kayttaja-lomake")
    fun getKayttajaForm(): ResponseEntity<KayttajahallintaKayttajaFormDTO> {
        val form = KayttajahallintaKayttajaFormDTO()

        form.yliopistot = yliopistoService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAllByLiittynytElsaan().toMutableSet()

        form.asetukset = asetusService.findAll().toMutableSet()

        form.opintooppaat = opintoopasService.findAll().toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/erikoistuvat-laakarit")
    fun createErikoistuvaLaakari(
        @Valid @RequestBody kayttajahallintaErikoistuvaLaakariDTO: KayttajahallintaErikoistuvaLaakariDTO,
    ): ResponseEntity<ErikoistuvaLaakariDTO> {
        if (userService.existsByEmail(kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite!!)) {
            throw BadRequestAlertException(
                "Samalla sähköpostilla löytyy jo toinen käyttäjä.",
                ENTITY_NAME,
                "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
            )
        }

        if (yliopistoService.findOne(kayttajahallintaErikoistuvaLaakariDTO.yliopistoId!!).isEmpty) {
            throw BadRequestAlertException(
                "Yliopistoa ei löydy.",
                ENTITY_NAME,
                "dataillegal.yliopistoa-ei-loydy"
            )
        }

        if (erikoisalaService.findOne(kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId!!).isEmpty) {
            throw BadRequestAlertException(
                "Erikoisalaa ei löydy.",
                ENTITY_NAME,
                "dataillegal.erikoisalaa-ei-loydy"
            )
        }

        if (asetusService.findOne(kayttajahallintaErikoistuvaLaakariDTO.asetusId!!) == null) {
            throw BadRequestAlertException(
                "Asetusta ei löydy.",
                ENTITY_NAME,
                "dataillegal.asetusta-ei-loydy"
            )
        }

        if (opintoopasService.findOne(kayttajahallintaErikoistuvaLaakariDTO.opintoopasId!!) == null) {
            throw BadRequestAlertException(
                "Opinto-opasta ei löydy.",
                ENTITY_NAME,
                "dataillegal.opinto-opasta-ei-loydy"
            )
        }

        val result = erikoistuvaLaakariService.save(kayttajahallintaErikoistuvaLaakariDTO)

        return ResponseEntity
            .created(URI("/api/tekninen-paakayttaja/kayttajat/${result.kayttajaId}"))
            .body(result)

    }

    @PutMapping("/erikoistuvat-laakarit/{id}/kutsu")
    fun resendErikoistuvaLaakariInvitation(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        erikoistuvaLaakariService.resendInvitation(id)

        return ResponseEntity
            .noContent()
            .build()
    }

    @PatchMapping("/kayttajat/{id}/aktivoi")
    fun activateKayttaja(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        kayttajaService.activateKayttaja(id)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/kayttajat/{id}/passivoi")
    fun passivateKayttaja(
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        kayttajaService.passivateKayttaja(id)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/kayttajat/{userId}")
    fun patchEmailAddress(
        @PathVariable userId: String,
        @Valid @RequestBody kayttajaDTO: KayttajahallintaUpdateKayttajaDTO,
    ): ResponseEntity<Void> {
        kayttajaDTO.sahkoposti?.let {
            if (userService.existsByEmail(it)) {
                throw BadRequestAlertException(
                    "Sähköpostiosoitteella löytyy jo käyttäjä",
                    "user",
                    "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
                )
            }
            userService.updateEmail(it, userId)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

        return ResponseEntity.ok().build()
    }

    private fun hasVirkailijaRole(userDTO: UserDTO): Boolean {
        return userDTO.authorities!!.contains(OPINTOHALLINNON_VIRKAILIJA)
    }
}
