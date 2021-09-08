package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.KoejaksonVaiheetService
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
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
    private val kayttajaService: KayttajaService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val koejaksonVastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @GetMapping("/koejaksot")
    fun getKoejaksot(principal: Principal?): ResponseEntity<List<KoejaksonVaiheDTO>> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val koejaksonVaiheet = koejaksonVaiheetService.findAllByVastuuhenkiloKayttajaId(kayttaja.id!!)
        return ResponseEntity.ok(koejaksonVaiheet)
    }

    @GetMapping("/koejakso/koulutussopimus/{id}")
    fun getKoulutussopimus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val koulutussopimusDTO =
            koejaksonKoulutussopimusService.findOneByIdAndVastuuhenkiloKayttajaId(id, kayttaja.id!!)

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

        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val existingKoulutussopimusDTO =
            koejaksonKoulutussopimusService.findOne(koulutussopimusDTO.id!!)

        if (existingKoulutussopimusDTO.get().lahetetty != true) {
            throw BadRequestAlertException(
                "Vastuuhenkilö ei saa muokata sopimusta, jos erikoistuva ei ole allekirjoittanut sitä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }

        if (existingKoulutussopimusDTO.get().kouluttajat?.any { it.sopimusHyvaksytty == false } == true) {
            throw BadRequestAlertException(
                "Vastuuhenkilö ei saa muokata sopimusta, jos kouluttajat eivät ole allekirjoittaneet sitä",
                ENTITY_KOEJAKSON_SOPIMUS,
                "dataillegal"
            )
        }

        val result =
            koejaksonKoulutussopimusService.update(koulutussopimusDTO, kayttaja.id!!)

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
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVastuuhenkiloId(id, kayttaja.id!!)

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

        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val vastuuhenkilonArvio =
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVastuuhenkiloId(
                vastuuhenkilonArvioDTO.id!!,
                kayttaja.id!!
            )

        if (!vastuuhenkilonArvio.isPresent) {
            throw BadRequestAlertException(
                "Koejakson vastuuhenkilön arviota ei löydy.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal"
            )
        }

        if (vastuuhenkilonArvio.get().erikoistuvaAllekirjoittanut == true) {
            throw BadRequestAlertException(
                "Allekirjoitettua arviointia ei saa muokata.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal"
            )
        }

        val result =
            koejaksonVastuuhenkilonArvioService.update(vastuuhenkilonArvioDTO, kayttaja.id!!)

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
