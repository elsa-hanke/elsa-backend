package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.service.KoejaksonVaiheetService
import fi.elsapalvelu.elsa.service.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.criteria.KoejaksoCriteria
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.jhipster.web.util.ResponseUtil
import java.security.Principal
import javax.validation.Valid

private const val ENTITY_KOEJAKSON_VASTUUHENKILON_ARVIO = "koejakson_vastuuhenkilon_arvio"

@RestController
@RequestMapping("/api/virkailija")
class VirkailijaKoejaksoResource(
    private val userService: UserService,
    private val koejaksonVaiheetService: KoejaksonVaiheetService,
    private val koejaksonVastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService
) {

    @GetMapping("/koejaksot")
    fun getKoejaksot(
        principal: Principal?,
        criteria: KoejaksoCriteria,
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
                "dataillegal.allekirjoitettua-arviointia-ei-saa-muokata"
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
