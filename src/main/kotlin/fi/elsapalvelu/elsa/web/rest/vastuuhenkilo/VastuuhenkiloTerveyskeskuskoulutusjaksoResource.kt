package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoUpdateDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

private const val TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME = "terveyskeskuskoulutusjakson_hyvaksynta"

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloTerveyskeskuskoulutusjaksoResource(
    private val userService: UserService,
    private val kayttajaService: KayttajaService,
    private val terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService
) {

    @GetMapping("/onko-terveyskeskuskoulutusjakso-vastuuhenkilo")
    fun getOnkoTerveyskeskuskoulutusjaksoVastuuhenkilo(principal: Principal?): ResponseEntity<Boolean> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        return ResponseEntity.ok(kayttaja.get().yliopistotAndErikoisalat?.any {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                .contains(VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN)
        })
    }

    @GetMapping("/terveyskeskuskoulutusjaksot")
    fun getTerveyskeskuskoulutusjaksot(
        principal: Principal?,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): ResponseEntity<Page<TerveyskeskuskoulutusjaksoSimpleDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByVastuuhenkiloUserId(
                user.id!!,
                criteria,
                pageable
            )
        )
    }

    @GetMapping("/terveyskeskuskoulutusjakso/{id}")
    fun getTerveyskeskuskoulutusjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!).get()
        val yliopistoIds = kayttaja.yliopistotAndErikoisalat?.filter {
            it.vastuuhenkilonTehtavat.map { tehtava -> tehtava.nimi }
                .contains(VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN)
        }?.map { it.yliopisto?.id!! }.orEmpty()
        try {
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByIdAndYliopistoIdVastuuhenkilo(
                id,
                yliopistoIds
            )
                .let {
                    if (it == null) return ResponseEntity.notFound().build()
                    return ResponseEntity.ok(it)
                }
        } catch (e: EntityNotFoundException) {
            throw BadRequestAlertException(
                "Vastuuhenkilöä ei löytynyt",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.vastuuhenkiloa-ei-loytynyt"
            )
        } catch (e: ValidationException) {
            throw BadRequestAlertException(
                "Terveyskeskuskoulutusjakson vähimmäispituus ei täyty",
                TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME,
                "dataillegal.terveyskeskuskoulutusjakson-vahimmaispituus-ei-tayty"
            )
        }
    }

    @PutMapping("/terveyskeskuskoulutusjakson-hyvaksynta/{id}")
    fun updateTerveyskeskuskoulutusjaksonHyvaksynta(
        @PathVariable id: Long,
        @RequestBody dto: TerveyskeskuskoulutusjaksoUpdateDTO?,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)

        terveyskeskuskoulutusjaksonHyvaksyntaService.update(
            user.id!!,
            false,
            id,
            dto?.korjausehdotus,
            dto?.lisatiedotVirkailijalta
        )
            .let {
                return ResponseEntity.ok(it)
            }
    }
}
