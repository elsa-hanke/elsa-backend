package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.web.rest.toFileDownloadResponse
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import java.security.Principal
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
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVaiheDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.jhipster.web.util.ResponseUtil
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
            koejaksonVaiheetService.findAllByVirkailijaKayttajaUserId(user.id.required(), criteria, pageable)
        return ResponseEntity.ok(koejaksonVaiheet)
    }

    @GetMapping("/koejakso/vastuuhenkilonarvio/{id}")
    fun getVastuuhenkilonArvio(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KoejaksonVastuuhenkilonArvioDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioService.findOneByIdAndVirkailijaUserId(id, user.id.required())
        return ResponseUtil.wrapOrNotFound(vastuuhenkilonArvioDTO)
    }

    @GetMapping("/koejakso/tyoskentelyjakso-liite/{id}")
    fun getVastuuhenkilonArvioTyoskentelyjaksoLiite(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id.required())
        val asiakirja = asiakirjaService
            .findByIdAndLiitettykoejaksoonByYliopisto(
                id,
                kayttaja.orElse(null)?.yliopistot?.map { it.id.required() })

        return asiakirja?.asiakirjaData?.fileInputStream
            ?.toFileDownloadResponse(asiakirja.nimi.orEmpty(), asiakirja.tyyppi.orEmpty())
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/koejakso/vastuuhenkilon-arvio/{id}/liite/{asiakirjaId}")
    fun getVastuuhenkilonArvioLiite(
        @PathVariable id: Long,
        @PathVariable asiakirjaId: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        return koejaksonVastuuhenkilonArvioService.findOneByIdAndVirkailijaUserId(id, user.id.required())
            .orElse(null)
            ?.asiakirjat?.firstOrNull { asiakirja -> asiakirja.id == asiakirjaId }
            ?.let { asiakirja ->
                asiakirjaService.findById(asiakirja.id.required())
                    ?.asiakirjaData?.fileInputStream
                    ?.toFileDownloadResponse(asiakirja.nimi.orEmpty(), asiakirja.tyyppi.orEmpty())
            }
            ?: ResponseEntity.notFound().build()
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
                vastuuhenkilonArvioDTO.id.required(),
                user.id.required()
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

        val result = koejaksonVastuuhenkilonArvioService.update(vastuuhenkilonArvioDTO, user.id.required())
        return ResponseEntity.ok(result)
    }
}
