package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.PoissaoloFormDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoFormDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import java.time.temporal.ChronoUnit
import javax.validation.Valid
import kotlin.math.ceil

private const val ENTITY_NAME = "tyoskentelyjakso"

@Suppress("unused")
@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariTyoskentelyjaksoResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val poissaolonSyyService: PoissaolonSyyService,
    private val keskeytysaikaService: KeskeytysaikaService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to create Tyoskentelyjakso : $tyoskentelyjaksoDTO")

        if (tyoskentelyjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä.",
                ENTITY_NAME,
                "idexists"
            )
        }
        val tyoskentelypaikka = tyoskentelyjaksoDTO.tyoskentelypaikka
        if (tyoskentelypaikka == null || tyoskentelypaikka.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä.",
                "tyoskentelypaikka",
                "idexists"
            )
        }
        if (tyoskentelyjaksoDTO.paattymispaiva != null
        ) {
            val daysBetween = ChronoUnit.DAYS.between(
                tyoskentelyjaksoDTO.alkamispaiva,
                tyoskentelyjaksoDTO.paattymispaiva
            ) + 1
            val minDays = ceil(
                (
                    30 * (
                        100 / tyoskentelyjaksoDTO.osaaikaprosentti!!
                            .coerceIn(50, 100)
                        )
                    ).toDouble()
            ).toInt()
            if (tyoskentelyjaksoDTO.paattymispaiva!!.isBefore(tyoskentelyjaksoDTO.alkamispaiva!!)) {
                throw BadRequestAlertException(
                    "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                    "tyoskentelypaikka",
                    "dataillegal"
                )
            } else if (daysBetween < minDays) {
                throw BadRequestAlertException(
                    "Työskentelyjakson minimikesto on 30 täyttä työpäivää",
                    "tyoskentelypaikka",
                    "dataillegal"
                )
            }
        }

        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksoService.save(tyoskentelyjaksoDTO, user.id!!)?.let {
            return ResponseEntity.created(URI("/api/tyoskentelyjaksot/${it.id}"))
                .headers(
                    HeaderUtil.createEntityCreationAlert(
                        applicationName,
                        true,
                        ENTITY_NAME,
                        it.id.toString()
                    )
                )
                .body(it)
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson täytyy olla oma.",
            ENTITY_NAME,
            "dataillegal"
        )
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to update Tyoskentelyjakso : $tyoskentelyjaksoDTO")

        if (tyoskentelyjaksoDTO.id == null) {
            throw BadRequestAlertException("Epäkelvollinen id", ENTITY_NAME, "idnull")
        }
        val user = userService.getAuthenticatedUser(principal)

        tyoskentelyjaksoService.save(tyoskentelyjaksoDTO, user.id!!)?.let {
            return ResponseEntity.ok()
                .headers(
                    HeaderUtil.createEntityUpdateAlert(
                        applicationName,
                        true,
                        ENTITY_NAME,
                        tyoskentelyjaksoDTO.id.toString()
                    )
                )
                .body(it)
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson täytyy olla oma.",
            ENTITY_NAME,
            "dataillegal"
        )
    }

    @GetMapping("/tyoskentelyjaksot/{id}")
    fun getTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to get Tyoskentelyjakso : $id")

        val user = userService.getAuthenticatedUser(principal)
        tyoskentelyjaksoService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        log.debug("REST request to delete Tyoskentelyjakso : $id")

        val user = userService.getAuthenticatedUser(principal)
        tyoskentelyjaksoService.delete(id, user.id!!)
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build()
    }

    @GetMapping("/tyoskentelyjakso-lomake")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoFormDTO> {
        log.debug("REST request to get TyoskentelyjaksoForm")

        val form = TyoskentelyjaksoFormDTO()

        form.kunnat = kuntaService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAll().toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/poissaolo-lomake")
    fun getPoissaoloForm(
        principal: Principal?
    ): ResponseEntity<PoissaoloFormDTO> {
        log.debug("REST request to get PoissaoloForm")

        val user = userService.getAuthenticatedUser(principal)

        val form = PoissaoloFormDTO()

        form.poissaoloSyyt = poissaolonSyyService.findAll().toMutableSet()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/tyoskentelyjaksot/poissaolot")
    fun createPoissaolo(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        log.debug("REST request to create Keskeytysaika : $keskeytysaikaDTO")

        if (keskeytysaikaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi keskeytysaika ei saa sisältää ID:tä.",
                "keskeytysaika",
                "idexists"
            )
        }
        val user = userService.getAuthenticatedUser(principal)

        keskeytysaikaService.save(keskeytysaikaDTO, user.id!!)?.let {
            return ResponseEntity.created(URI("/api/tyoskentelyjaksot/poissaolot/${it.id}"))
                .headers(
                    HeaderUtil.createEntityCreationAlert(
                        applicationName,
                        true,
                        "keskeytysaika",
                        it.id.toString()
                    )
                )
                .body(it)
        } ?: throw BadRequestAlertException(
            "Keskeytysajan täytyy olla oma.",
            "keskeytysaika",
            "dataillegal"
        )
    }

    @GetMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun getPoissaolo(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        log.debug("REST request to get Tyoskentelyjakso : $id")

        val user = userService.getAuthenticatedUser(principal)
        keskeytysaikaService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}
