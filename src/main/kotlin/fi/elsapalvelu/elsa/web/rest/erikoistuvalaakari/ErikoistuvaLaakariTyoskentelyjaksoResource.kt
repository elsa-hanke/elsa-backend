package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_NAME = "tyoskentelyjakso"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariTyoskentelyjaksoResource(
    private val kayttajaService: KayttajaService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val poissaolonSyyService: PoissaolonSyyService,
    private val keskeytysaikaService: KeskeytysaikaService,
    private val asiakirjaService: AsiakirjaService,
    private val objectMapper: ObjectMapper,
    private val fileValidator: FileValidatorService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.also {
            if (it.id != null) {
                throw BadRequestAlertException(
                    "Uusi tyoskentelyjakso ei saa sisältää ID:tä.",
                    ENTITY_NAME,
                    "idexists"
                )
            }
            val tyoskentelypaikka = it.tyoskentelypaikka
            if (tyoskentelypaikka == null || tyoskentelypaikka.id != null) {
                throw BadRequestAlertException(
                    "Uusi tyoskentelypaikka ei saa sisältää ID:tä.",
                    "tyoskentelypaikka",
                    "idexists"
                )
            }
        }?.also { validatePaattymispaiva(it) }?.let {
            val asiakirjaDTOs = getMappedFiles(files, kayttaja.id!!) ?: mutableSetOf()
            tyoskentelyjaksoService.create(it, kayttaja.id!!, asiakirjaDTOs)?.let { result ->
                return ResponseEntity.created(URI("/api/tyoskentelyjaksot/${result.id}"))
                    .headers(
                        HeaderUtil.createEntityCreationAlert(
                            applicationName,
                            true,
                            ENTITY_NAME,
                            result.id.toString()
                        )
                    )
                    .body(result)
            }

        } ?: throw BadRequestAlertException(
            "Työskentelyjakson lisääminen epäonnistui.",
            ENTITY_NAME,
            "dataillegal"
        )
    }

    private fun validatePaattymispaiva(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        tyoskentelyjaksoDTO.run {
            if (paattymispaiva != null
            ) {
                if (paattymispaiva!!.isBefore(alkamispaiva!!)) {
                    throw BadRequestAlertException(
                        "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                        "tyoskentelypaikka",
                        "dataillegal"
                    )
                }
            }
        }
    }

    private fun getMappedFiles(
        files: List<MultipartFile>?,
        kayttajaId: String
    ): MutableSet<AsiakirjaDTO>? {
        files?.let {
            fileValidator.validate(it, kayttajaId)
            return it.map { file ->
                AsiakirjaDTO(
                    nimi = file.originalFilename,
                    tyyppi = file.contentType,
                    asiakirjaData = AsiakirjaDataDTO(
                        fileInputStream = file.inputStream,
                        fileSize = file.size
                    )
                )
            }.toMutableSet()
        }

        return null
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.also {
            if (it.id == null) {
                throw BadRequestAlertException(
                    "Työskentelyjakson ID puuttuu.",
                    ENTITY_NAME,
                    "idnull"
                )
            }
        }?.also { validatePaattymispaiva(it) }?.let {
            val newAsiakirjat = getMappedFiles(files, kayttaja.id!!) ?: mutableSetOf()
            val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let { id ->
                objectMapper.readValue(id, mutableSetOf<Int>()::class.java)
            }
            tyoskentelyjaksoService.update(it, kayttaja.id!!, newAsiakirjat, deletedAsiakirjaIds)
                ?.let { result ->
                    return ResponseEntity.ok()
                        .headers(
                            HeaderUtil.createEntityUpdateAlert(
                                applicationName,
                                true,
                                ENTITY_NAME,
                                result.id.toString()
                            )
                        )
                        .body(result)
                }
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson päivittäminen epäonnistui.",
            ENTITY_NAME,
            "dataillegal"
        )
    }

    @GetMapping("/tyoskentelyjaksot-taulukko")
    fun getTyoskentelyjaksoTable(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksotTableDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val table = TyoskentelyjaksotTableDTO()
        table.poissaolonSyyt = poissaolonSyyService.findAll().toMutableSet()
        table.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaId(kayttaja.id!!).toMutableSet()
        table.keskeytykset = keskeytysaikaService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaId(kayttaja.id!!).toMutableSet()
        table.tilastot = tyoskentelyjaksoService.getTilastot(kayttaja.id!!)

        return ResponseEntity.ok(table)
    }

    @GetMapping("/tyoskentelyjaksot")
    fun getTyoskentelyjaksot(
        principal: Principal?
    ): ResponseEntity<List<TyoskentelyjaksoDTO>> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        val tyoskentelyjaksot =
            tyoskentelyjaksoService.findAllByErikoistuvaLaakariKayttajaId(kayttaja.id!!)

        return ResponseEntity.ok(tyoskentelyjaksot)
    }

    @GetMapping("/tyoskentelyjaksot/{id}")
    fun getTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        tyoskentelyjaksoService.findOne(id, kayttaja.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        asiakirjaService.removeTyoskentelyjaksoReference(kayttaja.id!!, id)
        tyoskentelyjaksoService.delete(id, kayttaja.id!!)

        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    ENTITY_NAME,
                    id.toString()
                )
            ).build()
    }

    @GetMapping("/tyoskentelyjakso-lomake")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoFormDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val form = TyoskentelyjaksoFormDTO()
        form.kunnat = kuntaService.findAll().toMutableSet()
        form.erikoisalat = erikoisalaService.findAll().toMutableSet()
        form.reservedAsiakirjaNimet =
            asiakirjaService.findAllByErikoistuvaLaakariId(kayttaja.id!!).map { it.nimi!! }
                .toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/poissaolo-lomake")
    fun getKeskeytysaikaForm(
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaFormDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)

        val form = KeskeytysaikaFormDTO()
        form.poissaolonSyyt = poissaolonSyyService.findAll().toMutableSet()
        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaId(kayttaja.id!!).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/tyoskentelyjaksot/poissaolot")
    fun createKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {

        if (keskeytysaikaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi keskeytysaika ei saa sisältää ID:tä.",
                "keskeytysaika",
                "idexists"
            )
        }
        if (keskeytysaikaDTO.alkamispaiva!!.isAfter(keskeytysaikaDTO.paattymispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei saa olla ennen alkamisaikaa",
                "keskeytysaika",
                "dataillegal"
            )
        }

        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        keskeytysaikaService.save(keskeytysaikaDTO, kayttaja.id!!)?.let {
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
            "Keskeytysajan lisääminen epäonnistui.",
            "keskeytysaika",
            "dataillegal"
        )
    }

    @PutMapping("/tyoskentelyjaksot/poissaolot")
    fun updateKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        if (keskeytysaikaDTO.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }

        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        keskeytysaikaService.save(keskeytysaikaDTO, kayttaja.id!!)?.let {
            return ResponseEntity.ok()
                .headers(
                    HeaderUtil.createEntityUpdateAlert(
                        applicationName,
                        true,
                        "keskeytysaika",
                        keskeytysaikaDTO.id.toString()
                    )
                )
                .body(it)
        } ?: throw BadRequestAlertException(
            "Keskeytysajan päivittäminen epäonnistui.",
            ENTITY_NAME,
            "dataillegal"
        )
    }

    @GetMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun getKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        keskeytysaikaService.findOne(id, kayttaja.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun deleteKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        keskeytysaikaService.delete(id, kayttaja.id!!)
        return ResponseEntity.noContent()
            .headers(
                HeaderUtil.createEntityDeletionAlert(
                    applicationName,
                    true,
                    "keskeytysaika",
                    id.toString()
                )
            )
            .build()
    }

    @PatchMapping("/tyoskentelyjaksot/koejakso")
    fun updateLiitettyKoejaksoon(
        @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO?> {
        val kayttaja = kayttajaService.getAuthenticatedKayttaja(principal)
        if (tyoskentelyjaksoDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }

        if (tyoskentelyjaksoDTO.liitettyKoejaksoon == null) {
            throw BadRequestAlertException(
                "liitettyKoejaksoon on pakollinen tieto",
                ENTITY_NAME,
                "illegaldata"
            )
        }

        tyoskentelyjaksoService.updateLiitettyKoejaksoon(
            tyoskentelyjaksoDTO.id!!,
            kayttaja.id!!,
            tyoskentelyjaksoDTO.liitettyKoejaksoon!!
        )?.let {
            val response = ResponseEntity.ok()
                .headers(
                    HeaderUtil.createEntityUpdateAlert(
                        applicationName,
                        true,
                        "tyoskentelyjakso",
                        it.id.toString()
                    )
                )
            return if (tyoskentelyjaksoDTO.liitettyKoejaksoon!!) response.body(it) else response.build()
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson päivittäminen epäonnistui.",
            ENTITY_NAME,
            "dataillegal"
        )
    }
}
