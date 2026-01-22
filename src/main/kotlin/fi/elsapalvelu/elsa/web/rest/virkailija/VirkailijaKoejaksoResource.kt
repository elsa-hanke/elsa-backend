package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.jhipster.web.util.ResponseUtil
import java.net.URLEncoder
import java.security.Principal
import jakarta.validation.Valid

private const val ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO = "koejakson_vastuuhenkilon_arvio"

@RestController
@RequestMapping("/api/virkailija")
class VirkailijaKoejaksoResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val koejaksonVastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService,
    private val asiakirjaService: AsiakirjaService
) {

    @GetMapping("/koejaksot")
    fun getKoejaksot(
        principal: Principal?,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): ResponseEntity<Page<KoejaksonVaiheDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val koejaksonVaiheet =
            koejaksonVaiheetService.findAllByVirkailijaKayttajaUserId(user.id!!, criteria, pageable)
        return ResponseEntity.ok(koejaksonVaiheet)
    }

    @GetMapping("/koejakso/vastuuhenkilonarvio/{id}")
    fun getVastuuhenkilonArvio(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVirkailijaUserId(id, user.id!!)
        return ResponseUtil.wrapOrNotFound(vastuuhenkilonArvioDTO)
    }

    @GetMapping("/koejakso/tyoskentelyjakso-liite/{id}")
    fun getVastuuhenkilonArvioTyoskentelyjaksoLiite(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        val asiakirja = asiakirjaService
            .findByIdAndLiitettykoejaksoonByYliopisto(
                id,
                kayttaja.orElse(null)?.yliopistot?.map { it.id!! })

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

    @GetMapping("/koejakso/vastuuhenkilon-arvio/{id}/liite/{asiakirjaId}")
    fun getVastuuhenkilonArvioLiite(
        @PathVariable id: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        koejaksonVastuuhenkilonArvioService.findOneByIdAndVirkailijaUserId(id, user.id!!)
            .orElse(null)?.let {
            it.asiakirjat?.firstOrNull { asiakirja -> asiakirja.id == asiakirjaId }
                ?.let { asiakirja ->
                    asiakirjaService.findById(asiakirja.id!!)?.let { asiakirjaWithData ->
                        asiakirjaWithData.asiakirjaData?.fileInputStream?.use { data ->
                            return ResponseEntity.ok()
                                .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + URLEncoder.encode(
                                        asiakirja.nimi,
                                        "UTF-8"
                                    ) + "\""
                                )
                                .header(
                                    HttpHeaders.CONTENT_TYPE,
                                    asiakirja.tyyppi + "; charset=UTF-8"
                                )
                                .body(data.readBytes())
                        }
                    }
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
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVirkailijaUserId(
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

        if (vastuuhenkilonArvio.get().virkailija?.sopimusHyvaksytty == true) {
            throw BadRequestAlertException(
                "Hyväksyttyä arviointia ei saa muokata.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal.hyvaksyttya-arviointia-ei-saa-muokata"
            )
        }

        if (vastuuhenkilonArvio.get().vastuuhenkilo?.sopimusHyvaksytty == true) {
            throw BadRequestAlertException(
                "Hyväksyttyä arviointia ei saa muokata.",
                ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO,
                "dataillegal.hyvaksyttya-arviointia-ei-saa-muokata"
            )
        }

        val result = koejaksonVastuuhenkilonArvioService.update(vastuuhenkilonArvioDTO, user.id!!)
        return ResponseEntity.ok(result)
    }
}
