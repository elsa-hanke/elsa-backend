package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.KoejaksonVaiheetService
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_KOEJAKSON_SOPIMUS = "koejakson_koulutussopimus"
private const val ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO = "koejakson_vastuuhenkilon_arvio"

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloKoejaksoResource(
    private val userService: UserService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val koejaksonVastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejaksot")
    fun getKoejaksot(principal: Principal?): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val koejaksonVaiheet = koejaksonVaiheetService.findAllByVastuuhenkiloKayttajaUserId(user.id!!)
        return ResponseEntity.ok(koejaksonVaiheet)
    }

    @GetMapping("/koejakso/koulutussopimus/{id}")
    fun getKoulutussopimus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get Koulutussopimus $id for user: $user.id")
        val koulutussopimusDTO =
            koejaksonKoulutussopimusService.findOneByIdAndVastuuhenkiloKayttajaUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(koulutussopimusDTO)
    }

    @PutMapping("/koejakso/koulutussopimus")
    fun updateKoulutussopimus(
        @Valid @RequestBody koulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        if (koulutussopimusDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_KOEJAKSON_SOPIMUS, "idnull")
        }

        val user = userService.getAuthenticatedUser(principal)

        val existingKoulutussopimusDTO =
            koejaksonKoulutussopimusService.findOne(koulutussopimusDTO.id!!)

        if (existingKoulutussopimusDTO.get().lahetetty != true) {
            throw BadRequestAlertException(
                "Vastuuhenkilö ei saa muokata sopimusta, jos erikoistuva ei ole allekirjoittanut sitä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.vastuuhenkilo-ei-saa-muokata-sopimusta-jos-erikoistuva-ei-ole-allekirjoittanut-sita"
            )
        }

        if (existingKoulutussopimusDTO.get().kouluttajat?.any { it.sopimusHyvaksytty == false } == true) {
            throw BadRequestAlertException(
                "Vastuuhenkilö ei saa muokata sopimusta, jos kouluttajat eivät ole allekirjoittaneet sitä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal.vastuuhenkilo-ei-saa-muokata-sopimusta-jos-kouluttajat-eivat-ole-allekirjottaneet-sita"
            )
        }

        val result =
            koejaksonKoulutussopimusService.update(koulutussopimusDTO, user.id!!)
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
    }

    @GetMapping("/koejakso/vastuuhenkilonarvio/{id}")
    fun getVastuuhenkilonArvio(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)

        log.debug("REST request to get vastuuhenkilon arvio $id for user: $user.id")
        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVastuuhenkiloUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(vastuuhenkilonArvioDTO)
    }

    @PutMapping("/koejakso/vastuuhenkilonarvio")
    fun updateVastuuhenkilonArvio(
        @Valid @RequestBody vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        if (vastuuhenkilonArvioDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "idnull"
            )
        }

        val user = userService.getAuthenticatedUser(principal)

        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVastuuhenkiloUserId(
                vastuuhenkilonArvioDTO.id!!,
                user.id!!
            )

        if (!vastuuhenkilonArvio.isPresent) {
            throw BadRequestAlertException(
                "Koejakson vastuuhenkilön arviota ei löydy.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal.koejakson-vastuuhenkilon-arviota-ei-loydy"
            )
        }

        if (vastuuhenkilonArvio.get().erikoistuvaAllekirjoittanut == true) {
            throw BadRequestAlertException(
                "Allekirjoitettua arviointia ei saa muokata.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal.allekirjoitettua-arviointia-ei-saa-muokata"
            )
        }

        val result =
            koejaksonVastuuhenkilonArvioService.update(vastuuhenkilonArvioDTO, user.id!!)
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(
                    applicationName,
                    true,
                    ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                    vastuuhenkilonArvioDTO.id.toString()
                )
            )
            .body(result)
    }
}
