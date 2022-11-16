package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.AvoinAndNimiCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.jhipster.web.util.ResponseUtil
import java.net.URLEncoder
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_KOEJAKSON_SOPIMUS = "koejakson_koulutussopimus"
private const val ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO = "koejakson_vastuuhenkilon_arvio"

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloKoejaksoResource(
    private val userService: UserService,
    private val koejaksonKoulutussopimusService: KoejaksonKoulutussopimusService,
    private val koejaksonAloituskeskusteluService: KoejaksonAloituskeskusteluService,
    private val koejaksonValiarviointiService: KoejaksonValiarviointiService,
    private val koejaksonKehittamistoimenpiteetService: KoejaksonKehittamistoimenpiteetService,
    private val koejaksonLoppukeskusteluService: KoejaksonLoppukeskusteluService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val koejaksonVastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService,
    private val asiakirjaService: AsiakirjaService
) {

    @GetMapping("/koejaksot")
    fun getKoejaksot(
        principal: Principal?,
        criteria: AvoinAndNimiCriteria,
        pageable: Pageable
    ): ResponseEntity<Page<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val koejaksonVaiheet =
            koejaksonVaiheetService.findAllByVastuuhenkiloKayttajaUserId(user.id!!, criteria, pageable)
        return ResponseEntity.ok(koejaksonVaiheet)
    }

    @GetMapping("/koejakso/koulutussopimus/{id}")
    fun getKoulutussopimus(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKoulutussopimusDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val koulutussopimusDTO =
            koejaksonKoulutussopimusService.findOneByIdAndVastuuhenkiloKayttajaUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(koulutussopimusDTO)
    }

    @GetMapping("/koejakso/aloituskeskustelu/{id}")
    fun getAloituskeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonAloituskeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val aloituskeskusteluDTO =
            koejaksonAloituskeskusteluService.findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
                id,
                user.id!!
            )
        return ResponseUtil.wrapOrNotFound(aloituskeskusteluDTO)
    }

    @GetMapping("/koejakso/valiarviointi/{id}")
    fun getValiarviointi(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonValiarviointiDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val valiarviointiDTO =
            koejaksonValiarviointiService.findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
                id,
                user.id!!
            )
        return ResponseUtil.wrapOrNotFound(valiarviointiDTO)
    }

    @GetMapping("/koejakso/kehittamistoimenpiteet/{id}")
    fun getKehittamistoimenpiteet(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonKehittamistoimenpiteetDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetService.findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
                id,
                user.id!!
            )
        return ResponseUtil.wrapOrNotFound(kehittamistoimenpiteetDTO)
    }

    @GetMapping("/koejakso/loppukeskustelu/{id}")
    fun getLoppukeskustelu(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonLoppukeskusteluDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val loppukeskusteluDTO =
            koejaksonLoppukeskusteluService.findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
                id,
                user.id!!
            )
        return ResponseUtil.wrapOrNotFound(loppukeskusteluDTO)
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
        return ResponseEntity.ok(result)
    }

    @GetMapping("/koejakso/vastuuhenkilonarvio/{id}")
    fun getVastuuhenkilonArvio(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVastuuhenkiloUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(vastuuhenkilonArvioDTO)
    }

    @GetMapping("/koejakso/vastuuhenkilonarvio/{id}/tyoskentelyjakso-liite/{asiakirjaId}")
    fun getVastuuhenkilonArvioTyoskentelyjaksoLiite(
        @PathVariable id: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        if (koejaksonVastuuhenkilonArvioService.existsByIdAndVastuuhenkiloUserId(id, user.id!!)) {
            val asiakirja = asiakirjaService.findByIdAndLiitettykoejaksoon(asiakirjaId)

            asiakirja?.asiakirjaData?.fileInputStream?.use {
                return ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(
                            asiakirja.nimi,
                            "UTF-8"
                        ) + "\""
                    )
                    .header(HttpHeaders.CONTENT_TYPE, asiakirja.tyyppi + "; charset=UTF-8")
                    .body(it.readBytes())
            }
        }

        return ResponseEntity.notFound().build()
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

        if (vastuuhenkilonArvio.get().allekirjoitettu == true) {
            throw BadRequestAlertException(
                "Allekirjoitettua arviointia ei saa muokata.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal.allekirjoitettua-arviointia-ei-saa-muokata"
            )
        }

        val result = koejaksonVastuuhenkilonArvioService.update(vastuuhenkilonArvioDTO, user.id!!)
        return ResponseEntity.ok(result)
    }
}
