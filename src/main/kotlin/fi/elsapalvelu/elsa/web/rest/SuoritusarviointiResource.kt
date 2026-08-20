package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.service.kayttaja.UserService
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.koejakso.*
import fi.elsapalvelu.elsa.service.tyoskentely.*
import fi.elsapalvelu.elsa.service.arviointi.*
import fi.elsapalvelu.elsa.service.suoritteet.*
import fi.elsapalvelu.elsa.service.koulutus.*
import fi.elsapalvelu.elsa.service.seuranta.*
import fi.elsapalvelu.elsa.service.valmistuminen.*
import fi.elsapalvelu.elsa.service.kayttaja.*
import fi.elsapalvelu.elsa.service.perustiedot.*
import fi.elsapalvelu.elsa.service.criteria.ArvioitavaKokonaisuusCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.service.dto.tyoskentely.*
import fi.elsapalvelu.elsa.service.dto.arviointi.*
import fi.elsapalvelu.elsa.service.dto.suoritteet.*
import fi.elsapalvelu.elsa.service.dto.koulutus.*
import fi.elsapalvelu.elsa.service.dto.seuranta.*
import fi.elsapalvelu.elsa.service.dto.valmistuminen.*
import fi.elsapalvelu.elsa.service.dto.kayttaja.*
import fi.elsapalvelu.elsa.service.dto.perustiedot.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import tech.jhipster.web.util.ResponseUtil
import jakarta.validation.Valid

private const val ENTITY_NAME = "suoritusarviointi"

open class SuoritusarviointiResource(
    private val suoritusarviointiService: SuoritusarviointiService,
    private val suoritusarviointiQueryService: SuoritusarviointiQueryService,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
    private val fileValidationService: FileValidationService,
    private val kayttajaService: KayttajaService,
    private val arvioitavaKokonaisuusService: ArvioitavaKokonaisuusService
) {

    @GetMapping("/suoritusarvioinnit")
    fun getAllSuoritusarvioinnit(
        principal: Principal?
    ): ResponseEntity<List<SuoritusarviointiDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val suoritusarvioinnit =
            suoritusarviointiQueryService.findByKouluttajaOrVastuuhenkiloUserId(user.id.required())
        val avoimet = suoritusarvioinnit.filter { it.arviointiAika == null }
        val muut = suoritusarvioinnit.filter { it.arviointiAika != null }
        val sortedSuoritusarvioinnit =
            avoimet.sortedBy { it.tapahtumanAjankohta } + muut.sortedByDescending { it.tapahtumanAjankohta }

        return ResponseEntity.ok(sortedSuoritusarvioinnit)
    }

    @GetMapping("/arviointipyynnot")
    fun getEtusivuArviointipyynnot(
        principal: Principal?
    ): ResponseEntity<List<EtusivuArviointipyyntoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val arviointipyynnot =
            suoritusarviointiService.findAvoimetByKouluttajaOrVastuuhenkiloUserId(user.id.required())
                .sortedBy { it.tapahtumanAjankohta }.map {
                    EtusivuArviointipyyntoDTO(
                        id = it.id,
                        erikoistujanNimi = it.arvioinninSaaja?.nimi,
                        pyynnonAika = it.pyynnonAika
                    )
                }

        return ResponseEntity.ok(arviointipyynnot)
    }

    @GetMapping("/suoritusarvioinnit/{id}")
    fun getSuoritusarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val suoritusarviointiDTO =
            suoritusarviointiService.findOneByIdAndArvioinninAntajauserId(id, user.id.required())
        return ResponseUtil.wrapOrNotFound(suoritusarviointiDTO)
    }

    @GetMapping("/suoritusarvioinnit/{id}/arviointi-liite/{asiakirjaId}")
    fun getArviointiLiite(
        @PathVariable id: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val asiakirja = suoritusarviointiService
            .findAsiakirjaBySuoritusarviointiIdAndArvioinninAntajauserId(id, user.id.required(), asiakirjaId)

        return asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi.orEmpty(), asiakirja.tyyppi.orEmpty())
            ?: ResponseEntity.notFound().build()
    }

    @PutMapping("/suoritusarvioinnit")
    fun updateSuoritusarviointi(
        @Valid @RequestParam suoritusarviointiJson: String,
        @Valid @RequestParam arviointiFiles: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<SuoritusarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return suoritusarviointiJson.let {
            objectMapper.readValue(it, SuoritusarviointiDTO::class.java)
        }?.let { suoritusarviointiDTO ->
            validateDTO(suoritusarviointiDTO)
            val newAsiakirjat = getMappedFiles(arviointiFiles) ?: mutableSetOf()
            val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let { id ->
                objectMapper.readValue(id, mutableSetOf<Int>()::class.java)
            }
            val result = suoritusarviointiService.save(
                suoritusarviointiDTO,
                newAsiakirjat,
                deletedAsiakirjaIds,
                user.id.required()
            )
            ResponseEntity.ok(result)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/erikoisalat")
    fun getErikoisalat(
        principal: Principal?
    ): ResponseEntity<List<ErikoisalaDTO?>> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id.required()).get()
        return ResponseEntity.ok(kayttaja.yliopistotAndErikoisalat?.map { it.erikoisala })
    }

    @GetMapping("/arvioitavatkokonaisuudet")
    fun getArvioitavatKokonaisuudet(
        criteria: ArvioitavaKokonaisuusCriteria,
        pageable: Pageable,
        principal: Principal?
    ): ResponseEntity<Page<ArvioitavaKokonaisuusDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id.required()).get()
        if (kayttaja.yliopistotAndErikoisalat?.map { it.erikoisala?.id }
                ?.contains(criteria.erikoisalaId) == false) {
            throw BadRequestAlertException(
                "Käyttäjällä ei ole oikeutta katsella erikoisalan tietoja.",
                ENTITY_NAME,
                "dataillegal.kayttajalla-ei-oikeutta-erikoisalaan"
            )
        }
        return ResponseEntity.ok(
            arvioitavaKokonaisuusService.findAllByErikoisalaIdPaged(
                criteria.erikoisalaId,
                criteria.voimassaolevat,
                pageable
            )
        )
    }

    private fun getMappedFiles(arviointiFiles: List<MultipartFile>?): MutableSet<AsiakirjaDTO>? {
        return arviointiFiles?.let {
            if (!fileValidationService.validate(
                    it,
                    listOf(MediaType.APPLICATION_PDF_VALUE)
                )
            ) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }
            return it.map { file -> file.mapAsiakirja() }.toMutableSet()
        }
    }

    private fun validateDTO(suoritusarviointiDTO: SuoritusarviointiDTO) {
        if (suoritusarviointiDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", ENTITY_NAME, "idnull")
        }
        if (suoritusarviointiDTO.keskenerainen) {
            return
        }
        suoritusarviointiDTO.arvioitavatKokonaisuudet?.forEach { k ->
            if (k.arviointiasteikonTaso == null
                || suoritusarviointiDTO.sanallinenArviointi == null
            ) {
                throw BadRequestAlertException(
                    "Kouluttajan arvioinnin täytyy sisältää arviointiasteikon taso ja sanallinen arviointi",
                    ENTITY_NAME,
                    "dataillegal.kouluttajan-arvioinnin-taytyy-sisaltaa-arviointiasteikon-taso-ja-sanallinen-arviointi"
                )
            }
        }
    }
}
