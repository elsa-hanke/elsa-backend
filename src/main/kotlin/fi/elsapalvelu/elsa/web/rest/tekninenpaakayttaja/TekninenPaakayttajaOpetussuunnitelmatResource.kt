package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.extensions.isInRange
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import tech.jhipster.web.util.ResponseUtil
import java.net.URI
import jakarta.validation.Valid

private const val OPINTOOPAS_ENTITY_NAME = "opintoopas"
private const val ERIKOISALA_ENTITY_NAME = "erikoisala"
private const val ARVIOITAVAN_KOKONAISUUDEN_KATEGORIA_ENTITY_NAME =
    "arvioitavankokonaisuudenkategoria"
private const val ARVIOITAVA_KOKONAISUUS_ENTITY_NAME = "arvioitavakokonaisuus"
private const val SUORITTEEN_KATEGORIA_ENTITY_NAME = "suoritteenkategoria"
private const val SUORITE_ENTITY_NAME = "suorite"

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaOpetussuunnitelmatResource(
    private val erikoisalaService: ErikoisalaService,
    private val opintoopasService: OpintoopasService,
    private val arviointiasteikkoService: ArviointiasteikkoService,
    private val arvioitavanKokonaisuudenKategoriaService: ArvioitavanKokonaisuudenKategoriaService,
    private val arvioitavaKokonaisuusService: ArvioitavaKokonaisuusService,
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritteenKategoriaService: SuoritteenKategoriaService,
    private val suoriteService: SuoriteService,
    private val suoritemerkintaService: SuoritemerkintaService
) {
    @GetMapping("/erikoisalat")
    fun getErikoisalat(): ResponseEntity<List<ErikoisalaDTO>> {
        return ResponseEntity.ok(erikoisalaService.findAll())
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

    @GetMapping("/erikoisalat/{id}/arvioitavankokonaisuudenkategoriat")
    fun getArvioitavatKokonaisuudenKategoriat(@PathVariable id: Long): ResponseEntity<List<ArvioitavanKokonaisuudenKategoriaSimpleDTO>> {
        return ResponseEntity.ok(arvioitavanKokonaisuudenKategoriaService.findAllByErikoisalaId(id))
    }

    @GetMapping("/erikoisalat/{id}/arvioitavatkokonaisuudet")
    fun getArvioitavatKokonaisuudet(@PathVariable id: Long): ResponseEntity<List<ArvioitavanKokonaisuudenKategoriaDTO>> {
        return ResponseEntity.ok(
            arvioitavanKokonaisuudenKategoriaService.findAllByErikoisalaIdWithKokonaisuudet(
                id
            )
        )
    }

    @GetMapping("/arvioitavankokonaisuudenkategoria/{id}")
    fun getArvioitavanKokonaisuudenKategoria(@PathVariable id: Long): ResponseEntity<ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO> {
        return ResponseUtil.wrapOrNotFound(arvioitavanKokonaisuudenKategoriaService.findOne(id))
    }

    @PostMapping("/arvioitavankokonaisuudenkategoria")
    fun createArvioitavanKokonaisuudenKategoria(@Valid @RequestBody arvioitavanKokonaisuudenKategoriaDTO: ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO): ResponseEntity<ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO> {
        if (arvioitavanKokonaisuudenKategoriaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi kategoria ei saa sisältää id:tä",
                ARVIOITAVAN_KOKONAISUUDEN_KATEGORIA_ENTITY_NAME,
                "idexists"
            )
        }
        arvioitavanKokonaisuudenKategoriaService.save(arvioitavanKokonaisuudenKategoriaDTO).let {
            return ResponseEntity
                .created(URI("/api/tekninen-paakayttaja/arvioitavankokonaisuudenkategoria/${it.id}"))
                .body(it)
        }
    }

    @PutMapping("/arvioitavankokonaisuudenkategoria")
    fun updateArvioitavanKokonaisuudenKategoria(@Valid @RequestBody arvioitavanKokonaisuudenKategoriaDTO: ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO): ResponseEntity<ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO> {
        if (arvioitavanKokonaisuudenKategoriaDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ARVIOITAVAN_KOKONAISUUDEN_KATEGORIA_ENTITY_NAME,
                "idnull"
            )
        }
        return ResponseEntity.ok(
            arvioitavanKokonaisuudenKategoriaService.save(
                arvioitavanKokonaisuudenKategoriaDTO
            )
        )
    }

    @GetMapping("/arvioitavakokonaisuus/{id}")
    fun getArvioitavaKokonaisuus(@PathVariable id: Long): ResponseEntity<ArvioitavaKokonaisuusWithErikoisalaDTO> {
        return ResponseUtil.wrapOrNotFound(arvioitavaKokonaisuusService.findOne(id))
    }

    @PostMapping("/arvioitavakokonaisuus")
    fun createArvioitavaKokonaisuus(@Valid @RequestBody arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ResponseEntity<ArvioitavaKokonaisuusDTO> {
        validateArvioitavaKokonaisuusVoimassaolo(arvioitavaKokonaisuusDTO)
        arvioitavaKokonaisuusService.create(arvioitavaKokonaisuusDTO).let {
            return ResponseEntity
                .created(URI("/api/tekninen-paakayttaja/arvioitavakokonaisuus/${it.id}"))
                .body(it)
        }
    }

    @PutMapping("/arvioitavakokonaisuus")
    fun updateArvioitavaKokonaisuus(@Valid @RequestBody arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ResponseEntity<ArvioitavaKokonaisuusDTO> {
        if (arvioitavaKokonaisuusDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                ARVIOITAVA_KOKONAISUUS_ENTITY_NAME,
                "idnull"
            )
        }
        validateArvioitavaKokonaisuusVoimassaolo(arvioitavaKokonaisuusDTO)
        validateArvioitavaKokonaisuusArvioinnit(arvioitavaKokonaisuusDTO)
        return ResponseEntity.ok(arvioitavaKokonaisuusService.update(arvioitavaKokonaisuusDTO))
    }

    @GetMapping("/erikoisalat/{id}/suoritteenkategoriat")
    fun getSuoritteenKategoriat(@PathVariable id: Long): ResponseEntity<List<SuoritteenKategoriaSimpleDTO>> {
        return ResponseEntity.ok(suoritteenKategoriaService.findAllByErikoisalaId(id))
    }

    @GetMapping("/erikoisalat/{id}/suoritteet")
    fun getSuoritteet(@PathVariable id: Long): ResponseEntity<List<SuoritteenKategoriaDTO>> {
        return ResponseEntity.ok(
            suoritteenKategoriaService.findAllByErikoisalaIdWithKokonaisuudet(
                id
            )
        )
    }

    @GetMapping("/suoritteenkategoria/{id}")
    fun getSuoritteenKategoria(@PathVariable id: Long): ResponseEntity<SuoritteenKategoriaWithErikoisalaDTO> {
        return ResponseUtil.wrapOrNotFound(suoritteenKategoriaService.findOne(id))
    }

    @PostMapping("/suoritteenkategoria")
    fun createSuoritteenKategoria(@Valid @RequestBody suoritteenKategoriaDTO: SuoritteenKategoriaWithErikoisalaDTO): ResponseEntity<SuoritteenKategoriaWithErikoisalaDTO> {
        if (suoritteenKategoriaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi kategoria ei saa sisältää id:tä",
                SUORITTEEN_KATEGORIA_ENTITY_NAME,
                "idexists"
            )
        }
        suoritteenKategoriaService.save(suoritteenKategoriaDTO).let {
            return ResponseEntity
                .created(URI("/api/tekninen-paakayttaja/suoritteenkategoria/${it.id}"))
                .body(it)
        }
    }

    @PutMapping("/suoritteenkategoria")
    fun updateSuoritteenKategoria(@Valid @RequestBody suoritteenKategoriaDTO: SuoritteenKategoriaWithErikoisalaDTO): ResponseEntity<SuoritteenKategoriaWithErikoisalaDTO> {
        if (suoritteenKategoriaDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                SUORITTEEN_KATEGORIA_ENTITY_NAME,
                "idnull"
            )
        }
        return ResponseEntity.ok(
            suoritteenKategoriaService.save(suoritteenKategoriaDTO)
        )
    }

    @GetMapping("/suorite/{id}")
    fun getSuorite(@PathVariable id: Long): ResponseEntity<SuoriteWithErikoisalaDTO> {
        return ResponseUtil.wrapOrNotFound(suoriteService.findOne(id))
    }

    @PostMapping("/suorite")
    fun createSuorite(@Valid @RequestBody suoriteDTO: SuoriteWithErikoisalaDTO): ResponseEntity<SuoriteWithErikoisalaDTO> {
        validateSuoritteenVoimassaolo(suoriteDTO)
        suoriteService.create(suoriteDTO).let {
            return ResponseEntity
                .created(URI("/api/tekninen-paakayttaja/suorite/${it.id}"))
                .body(it)
        }
    }

    @PutMapping("/suorite")
    fun updateSuorite(@Valid @RequestBody suoriteDTO: SuoriteWithErikoisalaDTO): ResponseEntity<SuoriteWithErikoisalaDTO> {
        if (suoriteDTO.id == null) {
            throw BadRequestAlertException(
                "Virheellinen id",
                SUORITE_ENTITY_NAME,
                "idnull"
            )
        }
        validateSuoritteenVoimassaolo(suoriteDTO)
        validateSuoritemerkinnat(suoriteDTO)
        return ResponseEntity.ok(suoriteService.update(suoriteDTO))
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

    private fun validateArvioitavaKokonaisuusVoimassaolo(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO) {
        if (arvioitavaKokonaisuusDTO.voimassaoloLoppuu != null && arvioitavaKokonaisuusDTO.voimassaoloLoppuu!!.isBefore(
                arvioitavaKokonaisuusDTO.voimassaoloAlkaa
            )
        ) {
            throw BadRequestAlertException(
                "Arvioitavan kokonaisuuden voimassaolon päättymispäivä ei saa olla ennen alkamisaikaa",
                ARVIOITAVA_KOKONAISUUS_ENTITY_NAME,
                "dataillegal.arvioitavan-kokonaisuuden-voimassaolon-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
            )
        }
    }

    private fun validateArvioitavaKokonaisuusArvioinnit(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO) {
        if (suoritusarviointiService.existsByArvioitavaKokonaisuusId(arvioitavaKokonaisuusDTO.id!!)) {
            arvioitavaKokonaisuusService.findOne(arvioitavaKokonaisuusDTO.id!!).orElse(null)?.let {
                if (arvioitavaKokonaisuusDTO.voimassaoloAlkaa!!.isAfter(it.voimassaoloAlkaa) || arvioitavaKokonaisuusDTO.voimassaoloLoppuu?.isBefore(
                        it.voimassaoloLoppuu
                    ) == true
                ) {
                    throw BadRequestAlertException(
                        "Arvioitavan kokonaisuuden voimassaoloa ei saa lyhentää, jos siihen liittyy suoritusarviointeja",
                        ARVIOITAVA_KOKONAISUUS_ENTITY_NAME,
                        "dataillegal.arvioitavan-kokonaisuuden-voimassaoloa-ei-saa-lyhentaa"
                    )
                }
            }
        }
    }

    private fun validateSuoritteenVoimassaolo(suoriteDTO: SuoriteWithErikoisalaDTO) {
        if (suoriteDTO.voimassaolonPaattymispaiva != null && suoriteDTO.voimassaolonPaattymispaiva!!.isBefore(
                suoriteDTO.voimassaolonAlkamispaiva
            )
        ) {
            throw BadRequestAlertException(
                "Suoritteen voimassaolon päättymispäivä ei saa olla ennen alkamisaikaa",
                SUORITE_ENTITY_NAME,
                "dataillegal.suoritteen-voimassaolon-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
            )
        }
    }

    private fun validateSuoritemerkinnat(suoriteDTO: SuoriteWithErikoisalaDTO) {
        if (suoritemerkintaService.existsBySuoriteId(suoriteDTO.id!!)) {
            suoriteService.findOne(suoriteDTO.id!!).orElse(null)?.let {
                if (suoriteDTO.voimassaolonAlkamispaiva!!.isAfter(it.voimassaolonAlkamispaiva) || suoriteDTO.voimassaolonPaattymispaiva?.isBefore(
                        it.voimassaolonPaattymispaiva
                    ) == true
                ) {
                    throw BadRequestAlertException(
                        "Suoritteen voimassaoloa ei saa lyhentää, jos siihen liittyy suoritemerkintöjä",
                        SUORITE_ENTITY_NAME,
                        "dataillegal.suoritteen-voimassaoloa-ei-saa-lyhentaa"
                    )
                }
            }
        }
    }
}
