package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.KoejaksoService
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.UnauthorizedException
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoulutussopimusTila
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_KOEJAKSO = "koejakso"
private const val ENTITY_KOEJAKSON_SOPIMUS = "koejakson_koulutussopimus"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariKoejaksoResource(
    private val userService: UserService,
    private val koejaksoService: KoejaksoService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejakso")
    fun getKoejakso(
        principal: Principal?
    ): ResponseEntity<KoejaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get Koejakso for user: $user.id")
        koejaksoService.findByErikoistuvaLaakariKayttajaUserId(user.id!!).let {
            it.koulutusSopimuksenTila = KoulutussopimusTila.fromSopimus(it.koulutussopimus)
            return ResponseEntity.ok(it)
        }
    }

    @PutMapping("/koejakso/{koejaksoId}/tyoskentelyjaksot/{id}")
    fun addTyoskentelyjakso(
        @PathVariable koejaksoId: Long,
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        log.debug("REST request to add tyoskentelyjakso $id to koejakso $koejaksoId")
        val user = userService.getAuthenticatedUser(principal)
        val result = koejaksoService.addTyoskentelyjakso(koejaksoId, id, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSO,
                    koejaksoId.toString()
                )
            )
            .body(result)
    }

    @DeleteMapping("/koejakso/{koejaksoId}/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable koejaksoId: Long,
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        log.debug("REST request to remove tyoskentelyjakso $id from koejakso $koejaksoId")

        val user = userService.getAuthenticatedUser(principal)
        koejaksoService.removeTyoskentelyjakso(koejaksoId, id, user.id!!)

        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSO,
                    id.toString()
                )
            ).build()
    }

    @PostMapping("/koejakso/{id}/koulutussopimus")
    fun createKoulutussopimus(
        @PathVariable id: Long,
        @Valid @RequestBody koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        if (koulutussopimusDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi koulutussopimus ei saa sisältää ID:tä.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "idexists"
            )
        }
        validateKoulutussopimus(koulutussopimusDTO)

        try {
            val result = koejaksonKoulutussopimusService.save(koulutussopimusDTO, id, user.id!!)
            return ResponseEntity.created(URI("/api/suoritusarvioinnit/${result.id}"))
                .headers(
                    HeaderUtil.createEntityCreationAlert(
                        applicationName,
                        true,
                        ENTITY_KOEJAKSON_SOPIMUS,
                        result.id.toString()
                    )
                )
                .body(result)
        } catch (e: UnauthorizedException) {
            throw BadRequestAlertException(
                e.message.toString(),
                "koejakso",
                "dataillegal"
            )
        }
    }

    @PutMapping("/koejakso/{id}/koulutussopimus")
    fun updateKoulutussopimus(
        @PathVariable id: Long,
        @Valid @RequestBody koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        if (koulutussopimusDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_KOEJAKSON_SOPIMUS, "idnull")
        }

        validateKoulutussopimus(koulutussopimusDTO)

        val existingKoulutussopimusDTO =
            koejaksonKoulutussopimusService.findOne(koulutussopimusDTO.id!!)
        if (existingKoulutussopimusDTO.get().lahetetty == true) {
            throw BadRequestAlertException(
                "Lähetettyä koulutussopimusta ei saa muokata.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }

        val user = userService.getAuthenticatedUser(principal)

        try {
            val result =
                koejaksonKoulutussopimusService.save(koulutussopimusDTO, id, user.id!!)
            return ResponseEntity.ok()
                .headers(
                    HeaderUtil.createEntityUpdateAlert(
                        applicationName,
                        true,
                        ENTITY_KOEJAKSON_SOPIMUS,
                        koulutussopimusDTO.id.toString()
                    )
                )
                .body(result)
        } catch (e: UnauthorizedException) {
            throw BadRequestAlertException(
                e.message.toString(),
                "koejakso",
                "dataillegal"
            )
        }
    }

    private fun validateKoulutussopimus(koulutussopimusDTO: KoejaksonKoulutussopimusDTO) {
        if (koulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true || koulutussopimusDTO.vastuuhenkilo?.kuittausaika != null) {
            throw BadRequestAlertException(
                "Uusi koulutussopimus ei saa sisältää vastuuhenkilön kuittausta. Vastuuhenkilö määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }
        if (koulutussopimusDTO.kouluttajat?.any { k -> k.sopimusHyvaksytty == true || k.kuittausaika != null }!!) {
            throw BadRequestAlertException(
                "Uusi koulutussopimus ei saa sisältää kouluttajien kuittausta. Kouluttaja määrittelee sen.",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }
    }
}
