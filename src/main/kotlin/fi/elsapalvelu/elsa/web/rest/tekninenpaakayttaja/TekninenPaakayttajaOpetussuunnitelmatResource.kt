package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.extensions.isInRange
import fi.elsapalvelu.elsa.service.ArviointiasteikkoService
import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.OpintoopasService
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import tech.jhipster.web.util.ResponseUtil
import java.net.URI
import javax.validation.Valid

private const val OPINTOOPAS_ENTITY_NAME = "opintoopas"
private const val ERIKOISALA_ENTITY_NAME = "erikoisala"

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaOpetussuunnitelmatResource(
    private val erikoisalaService: ErikoisalaService,
    private val opintoopasService: OpintoopasService,
    private val arviointiasteikkoService: ArviointiasteikkoService
) {
    @GetMapping("/erikoisalat")
    fun getErikoisalat(): ResponseEntity<List<ErikoisalaDTO>> {
        return ResponseEntity.ok(erikoisalaService.findAllByLiittynytElsaan())
    }

    @GetMapping("/erikoisalat/{id}")
    fun getErikoisala(@PathVariable id: Long): ResponseEntity<ErikoisalaDTO> {
        return ResponseUtil.wrapOrNotFound(erikoisalaService.findOne(id))
    }

    @GetMapping("/erikoisalat/{id}/oppaat")
    fun getOpintooppaat(@PathVariable id: Long): ResponseEntity<List<OpintoopasSimpleDTO>> {
        return ResponseEntity.ok(opintoopasService.findAllByErikoisala(id))
    }

    @GetMapping("/erikoisalat/{id}/uusinopas")
    fun getUusinOpintoopas(@PathVariable id: Long): ResponseEntity<OpintoopasDTO> {
        return ResponseEntity.ok(opintoopasService.findUusinByErikoisala(id))
    }

    @GetMapping("/opintoopas/{id}")
    fun getOpintoopas(@PathVariable id: Long): ResponseEntity<OpintoopasDTO> {
        return ResponseEntity.ok(opintoopasService.findOne(id))
    }

    @PostMapping("/opintoopas")
    fun createOpintoopas(@Valid @RequestBody opintoopasDTO: OpintoopasDTO): ResponseEntity<OpintoopasDTO> {
        if (opintoopasDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi opinto-opas ei saa sisältää id:tä",
                OPINTOOPAS_ENTITY_NAME,
                "idexists"
            )
        }
        validateOpintoopas(opintoopasDTO)
        opintoopasService.update(opintoopasDTO)?.let {
            return ResponseEntity
                .created(URI("/api/tekninen-paakayttaja/opintoopas/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/opintoopas")
    fun updateOpintoopas(@Valid @RequestBody opintoopasDTO: OpintoopasDTO): ResponseEntity<OpintoopasDTO> {
        if (opintoopasDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                OPINTOOPAS_ENTITY_NAME,
                "idnull"
            )
        }
        validateOpintoopas(opintoopasDTO)
        return ResponseEntity.ok(opintoopasService.update(opintoopasDTO))
    }

    @GetMapping("/arviointiasteikot")
    fun getArviointiasteikot(): ResponseEntity<List<ArviointiasteikkoDTO>> {
        return ResponseEntity.ok(arviointiasteikkoService.findAll())
    }

    private fun validateOpintoopas(opintoopasDTO: OpintoopasDTO) {
        val erikoisalaId = opintoopasDTO.erikoisala?.id
            ?: throw BadRequestAlertException(
                "Opinto-oppaalta puuttuu tieto erikoisalasta.",
                ERIKOISALA_ENTITY_NAME,
                "dataillegal.opinto-oppaalta-puuttuu-erikoisala"
            )

        if (opintoopasDTO.voimassaoloPaattyy != null && opintoopasDTO.voimassaoloPaattyy!!.isBefore(
                opintoopasDTO.voimassaoloAlkaa
            )
        ) {
            throw BadRequestAlertException(
                "Opinto-oppaan voimassaolon päättymispäivä ei saa olla ennen alkamisaikaa",
                OPINTOOPAS_ENTITY_NAME,
                "dataillegal.opinto-oppaan-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
            )
        }

        val opintooppaat =
            opintoopasService.findAllByErikoisala(erikoisalaId).filter { it.id != opintoopasDTO.id }

        val uusin = opintooppaat.maxByOrNull { it.voimassaoloAlkaa!! } ?: return

        if (opintoopasDTO.voimassaoloPaattyy == null) {
            if (opintoopasDTO.voimassaoloAlkaa!!.isBefore(uusin.voimassaoloAlkaa)) {
                throw BadRequestAlertException(
                    "Opinto-oppaalle on annettava päättymispäivä, jos se ei ole uusin",
                    OPINTOOPAS_ENTITY_NAME,
                    "dataillegal.opinto-oppaalle-annettava-paattymispaiva"
                )
            } else if (uusin.voimassaoloPaattyy != null && opintoopasDTO.voimassaoloAlkaa!!.isBefore(
                    uusin.voimassaoloPaattyy
                )
            ) {
                throw BadRequestAlertException(
                    "Opinto-oppaan voimassaolo ei saa olla päällekkäinen toisen opinto-oppaan kanssa",
                    OPINTOOPAS_ENTITY_NAME,
                    "dataillegal.opinto-oppaan-voimassaolo-ei-saa-olla-paallekkainen"
                )
            }
        }

        val voimassaoloPaattyy = opintoopasDTO.voimassaoloPaattyy ?: return

        if (opintoopasDTO.voimassaoloAlkaa!!.isAfter(uusin.voimassaoloAlkaa) && uusin.voimassaoloPaattyy == null) {
            return
        }

        for (date in opintoopasDTO.voimassaoloAlkaa!!.datesUntil(voimassaoloPaattyy.plusDays(1))) {
            if (opintooppaat.any { date.isInRange(it.voimassaoloAlkaa!!, it.voimassaoloPaattyy) }) {
                throw BadRequestAlertException(
                    "Opinto-oppaan voimassaolo ei saa olla päällekkäinen toisen opinto-oppaan kanssa",
                    OPINTOOPAS_ENTITY_NAME,
                    "dataillegal.opinto-oppaan-voimassaolo-ei-saa-olla-paallekkainen"
                )
            }
        }
    }
}
