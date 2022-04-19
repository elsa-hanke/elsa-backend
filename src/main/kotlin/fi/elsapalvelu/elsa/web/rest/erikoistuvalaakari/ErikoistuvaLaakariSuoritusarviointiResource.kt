package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.SuoritusarviointiCriteria
import fi.elsapalvelu.elsa.service.dto.ArviointipyyntoFormDTO
import fi.elsapalvelu.elsa.service.dto.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinnitOptionsDTO
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.jhipster.web.util.ResponseUtil
import java.net.URI
import java.net.URLEncoder
import java.security.Principal
import java.time.LocalDate
import java.time.ZoneId
import javax.validation.Valid

private const val ENTITY_NAME = "suoritusarviointi"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariSuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService,
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val arvioitavaKokonaisuusService: ArvioitavaKokonaisuusService,
    private val kayttajaService: KayttajaService,
    private val arviointiasteikkoService: ArviointiasteikkoService,
    private val opintooikeusService: OpintooikeusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/suoritusarvioinnit-rajaimet")
    fun getSuoritusarvioinnitRajaimet(
        principal: Principal?
    ): ResponseEntity<SuoritusarvioinnitOptionsDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val options = SuoritusarvioinnitOptionsDTO()
        options.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeusId).toMutableSet()
        options.arvioitavatKokonaisuudet =
            arvioitavaKokonaisuusService.findAllByOpintooikeusId(opintooikeusId).toMutableSet()
        options.tapahtumat = suoritusarviointiService
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).toMutableSet()
        options.kouluttajatAndVastuuhenkilot =
            kayttajaService.findKouluttajatAndVastuuhenkilot(user.id!!).toMutableSet()

        return ResponseEntity.ok(options)
    }

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        criteria: SuoritusarviointiCriteria,
        principal: Principal?
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        return ResponseEntity.ok(
            suoritusarviointiQueryService
                .findByCriteriaAndTyoskentelyjaksoOpintooikeusId(criteria, opintooikeusId)
        )
    }

    @GetMapping("/arviointipyynto-lomake")
    fun getArviointipyyntoForm(
        principal: Principal?
    ): ResponseEntity<ArviointipyyntoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val form = ArviointipyyntoFormDTO()
        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeusId).toSet()
        form.kunnat = kuntaService.findAll().toSet()
        form.erikoisalat =
            erikoisalaService.findAll().toSet()
        form.arvioitavanKokonaisuudenKategoriat =
            arvioitavaKokonaisuusService.findAllByOpintooikeusId(opintooikeusId)
                .groupBy { it.kategoria }.map {
                    ArvioitavanKokonaisuudenKategoriaDTO(
                        it.key?.id,
                        it.key?.nimi,
                        it.key?.jarjestysnumero,
                        it.key?.voimassaoloAlkaa,
                        it.key?.voimassaoloLoppuu,
                        it.value.toSet()
                    )
                }.toSet()

        form.kouluttajatAndVastuuhenkilot =
            kayttajaService.findKouluttajatAndVastuuhenkilot(user.id!!).toSet()
        return ResponseEntity.ok(form)
    }

    @PostMapping("/suoritusarvioinnit/arviointipyynto")
    fun createSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        validateSuoritusarviointiDTO(suoritusarviointiDTO)

        val tyoskentelyjakso = tyoskentelyjaksoService
            .findOne(suoritusarviointiDTO.tyoskentelyjaksoId!!, opintooikeusId)
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)

        if (tyoskentelyjakso == null || kirjautunutErikoistuvaLaakari == null) {
            throw BadRequestAlertException(
                "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                ENTITY_NAME,
                "dataillegal.uuden-arviointipyynnon-pitaa-kohdistua-johonkin-erikoistuvan-tyoskentelyjaksoon"
            )
        }
        if (tyoskentelyjakso.alkamispaiva!! > suoritusarviointiDTO.tapahtumanAjankohta!! ||
            (
                tyoskentelyjakso.paattymispaiva != null &&
                    suoritusarviointiDTO.tapahtumanAjankohta!! > tyoskentelyjakso.paattymispaiva!!
                )
        ) {
            throw BadRequestAlertException(
                "Uuden arviointipyynnön pitää kohdistua työskentelyjakson väliin.",
                ENTITY_NAME,
                "dataillegal.uuden-arviointipyynnon-pitaa-kohdistua-tyoskentelyjakson-valiin"
            )
        }

        suoritusarviointiDTO.arviointiasteikko = arviointiasteikkoService.findByOpintooikeusId(opintooikeusId)
        suoritusarviointiDTO.pyynnonAika = LocalDate.now(ZoneId.systemDefault())

        val result = suoritusarviointiService.save(suoritusarviointiDTO)
        return ResponseEntity
            .created(URI("/api/suoritusarvioinnit/${result.id}"))
            .body(result)
    }

    private fun validateSuoritusarviointiDTO(suoritusarviointiDTO: SuoritusarviointiDTO) {
        if (suoritusarviointiDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää ID:tä",
                ENTITY_NAME,
                "idexists"
            )
        }
        if (suoritusarviointiDTO.arviointiasteikonTaso != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää arviointiasteikon tasoa. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-arviointipyynto-ei-saa-sisaltaa-arviointiasteikon-tasoa"
            )
        }
        if (suoritusarviointiDTO.vaativuustaso != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää vaativuustasoa. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-arviointipyynto-ei-saa-sisaltaa-vaativuustasoa"
            )
        }
        if (suoritusarviointiDTO.sanallinenArviointi != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää sanallista arviointia. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-arviointipyynto-ei-saa-sisltaa-sanallista-arviointia"
            )
        }
        if (suoritusarviointiDTO.arviointiAika != null) {
            throw BadRequestAlertException(
                "Uusi arviointipyyntö ei saa sisältää arvioinnin aikaa. Kouluttaja määrittelee sen.",
                ENTITY_NAME,
                "dataillegal.uusi-arviointipyynto-ei-saa-sisaltaa-arvioinnin-aikaa"
            )
        }
        if (suoritusarviointiDTO.tyoskentelyjaksoId == null) {
            throw BadRequestAlertException(
                "Uuden arviointipyynnön pitää kohdistua johonkin erikoistuvan työskentelyjaksoon.",
                ENTITY_NAME,
                "dataillegal.uuden-arviointipyynnon-pitaa-kohdistua-johonkin-erikoistuvan-tyoskentelyjaksoon"
            )
        }
    }

    @PutMapping("/suoritusarvioinnit")
    fun updateSuoritusarviointi(
        @Valid @RequestBody suoritusarviointiDTO: SuoritusarviointiDTO,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }

        if (suoritusarviointiDTO.arviointiasteikko != null) {
            throw IllegalArgumentException("Käytettyä arviointiasteikkoa ei voi muokata")
        }

        val user = userService.getAuthenticatedUser(principal)
        val result = suoritusarviointiService.save(suoritusarviointiDTO, user.id!!)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    fun getSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val suoritusarviointiDTO = suoritusarviointiService
            .findOneByIdAndTyoskentelyjaksoOpintooikeusId(id, opintooikeusId)
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @GetMapping("/suoritusarvioinnit/{id}/arviointi-liite")
    fun getArviointiLiite(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val asiakirja =
            suoritusarviointiService.findAsiakirjaBySuoritusarviointiIdAndTyoskentelyjaksoOpintooikeusId(
                id,
                opintooikeusId
            )

        asiakirja?.asiakirjaData?.fileInputStream?.use {
            return ResponseEntity.ok()
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(asiakirja.nimi, "UTF-8") + "\""
                )
                .header(HttpHeaders.CONTENT_TYPE, asiakirja.tyyppi + "; charset=UTF-8")
                .body(it.readBytes())
        }
        return ResponseEntity.notFound().build()
    }

    @DeleteMapping("/suoritusarvioinnit/{id}")
    fun deleteSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        suoritusarviointiService.delete(id, opintooikeusId)
        return ResponseEntity
            .noContent()
            .build()
    }
}
