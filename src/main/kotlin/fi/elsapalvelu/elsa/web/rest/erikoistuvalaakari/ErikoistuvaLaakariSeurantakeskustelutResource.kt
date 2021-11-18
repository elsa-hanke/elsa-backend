package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import java.time.LocalDate
import java.util.*
import javax.validation.Valid

private const val ENTITY_NAME = "seurantajakso"

@RestController
@RequestMapping("/api/erikoistuva-laakari/seurantakeskustelut")
class ErikoistuvaLaakariSeurantakeskustelutResource(
    private val userService: UserService,
    private val seurantajaksoService: SeurantajaksoService,
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritemerkintaService: SuoritemerkintaService,
    private val koulutusjaksoService: KoulutusjaksoService,
    private val teoriakoulutusService: TeoriakoulutusService
) {

    @GetMapping("/seurantajaksot")
    fun getSeurantajaksot(principal: Principal?): ResponseEntity<List<SeurantajaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            seurantajaksoService.findByErikoistuvaLaakariKayttajaUserId(user.id!!)
        )
    }

    @GetMapping("/seurantajakso/{id}")
    fun getSeurantajakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SeurantajaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return seurantajaksoService.findOne(id, user.id!!)?.let {
            ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/seurantajaksontiedot")
    fun getSeurantajaksonTiedot(
        principal: Principal?,
        @RequestParam alkamispaiva: LocalDate,
        @RequestParam paattymispaiva: LocalDate,
        @RequestParam koulutusjaksot: List<Long>
    ): ResponseEntity<SeurantajaksonTiedotDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            seurantajaksoService.findSeurantajaksonTiedot(
                user.id!!,
                alkamispaiva,
                paattymispaiva,
                koulutusjaksot
            )
        )
    }

    @PostMapping("/seurantajakso")
    fun createSeurantajakso(
        @Valid @RequestBody seurantajaksoDTO: SeurantajaksoDTO,
        principal: Principal?
    ): ResponseEntity<SeurantajaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        if (seurantajaksoDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi seurantajakso ei saa sisältää ID:tä",
                ENTITY_NAME,
                "idexists"
            )
        }
        if (seurantajaksoDTO.edistyminenTavoitteidenMukaista != null) {
            throw BadRequestAlertException(
                "Uusi seurantajakso ei saa sisältää edistymistä tavoitteiden mukaan. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-seurantajakso-ei-saa-sisaltaa-edistymistä-tavoitteiden-mukaisesti"
            )
        }
        if (seurantajaksoDTO.huolenaiheet != null) {
            throw BadRequestAlertException(
                "Uusi seurantajakso ei saa sisältää huolenaiheita. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-seurantajakso-ei-saa-sisaltaa-huolenaiheita"
            )
        }
        if (seurantajaksoDTO.kouluttajanArvio != null) {
            throw BadRequestAlertException(
                "Uusi seurantajakso ei saa sisältää kouluttajan arviota. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-seurantajakso-ei-saa-sisaltaa-kouluttajan-arviota"
            )
        }
        if (seurantajaksoDTO.erikoisalanTyoskentelyvalmiudet != null) {
            throw BadRequestAlertException(
                "Uusi seurantajakso ei saa sisältää erikoisalan työskentelyvalmiuksia. Kouluttaja määrittelee ne.",
                ENTITY_NAME,
                "dataillegal.uusi-seurantajakso-ei-saa-sisaltaa-erikoisalan-tyoskentelyvalmiuksia"
            )
        }
        if (seurantajaksoDTO.jatkotoimetJaRaportointi != null) {
            throw BadRequestAlertException(
                "Uusi seurantajakso ei saa sisältää jatkotoimia. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-seurantajakso-ei-saa-sisaltaa-jatkotoimia"
            )
        }
        if (seurantajaksoDTO.hyvaksytty != null) {
            throw BadRequestAlertException(
                "Uusi seurantajakso ei saa hyväksyntää.",
                ENTITY_NAME,
                "dataillegal.uusi-seurantajakso-ei-saa-sisaltaa-hyvaksyntaa"
            )
        }

        val result = seurantajaksoService.create(seurantajaksoDTO, user.id!!)
        return ResponseEntity
            .created(URI("/api/seurantakeskustelut/seurantajakso/${result.id}"))
            .body(result)
    }

    @PutMapping("/seurantajakso/{id}")
    fun updateSeurantajakso(
        @PathVariable(value = "id", required = false) id: Long,
        @Valid @RequestBody seurantajaksoDTO: SeurantajaksoDTO,
        principal: Principal?
    ): ResponseEntity<SeurantajaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        if (seurantajaksoDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ErikoistuvaLaakariKoulutusjaksoResource.ENTITY_NAME, "idnull"
            )
        }

        if (!Objects.equals(id, seurantajaksoDTO.id)) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ErikoistuvaLaakariKoulutusjaksoResource.ENTITY_NAME, "idinvalid"
            )
        }

        val seurantajakso = seurantajaksoService.findOne(id, user.id!!)
            ?: throw BadRequestAlertException(
                "Seurantajaksoa ei löydy",
                ErikoistuvaLaakariKoulutusjaksoResource.ENTITY_NAME, "idnotfound"
            )

        if (seurantajakso.hyvaksytty == true) {
            throw BadRequestAlertException(
                "Hyväksyttyä seurantajaksoa ei saa päivittää",
                ErikoistuvaLaakariKoulutusjaksoResource.ENTITY_NAME,
                "dataillegal.hyvaksyttya-seurantajaksoa-ei-saa-paivittaa"
            )
        }

        if (seurantajakso.seurantakeskustelunYhteisetMerkinnat != null && seurantajakso.korjausehdotus == null) {
            throw BadRequestAlertException(
                "Yhteiset merkinnät kirjattua seurantajaksoa ei saa päivittää, ellei seurantajakso ole palautettu",
                ErikoistuvaLaakariKoulutusjaksoResource.ENTITY_NAME,
                "dataillegal.yhteiset-merkinnat-kirjattua-seurantajaksoa-ei-saa-paivittaa"
            )
        }

        val result = seurantajaksoService.update(seurantajaksoDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/seurantajakso/{id}")
    fun deleteSeurantajakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)

        val seurantajakso = seurantajaksoService.findOne(id, user.id!!)

        if (seurantajakso?.seurantakeskustelunYhteisetMerkinnat != null) {
            throw BadRequestAlertException(
                "Keskustelut käytyä seurantajaksoa ei saa poistaa",
                ErikoistuvaLaakariKoulutusjaksoResource.ENTITY_NAME,
                "dataillegal.arvioitua-tai-keskustelut-merkittya-seurantajaksoa-ei-saa-poistaa"
            )
        }
        seurantajaksoService.delete(id, user.id!!)
        return ResponseEntity.noContent().build()
    }
}
