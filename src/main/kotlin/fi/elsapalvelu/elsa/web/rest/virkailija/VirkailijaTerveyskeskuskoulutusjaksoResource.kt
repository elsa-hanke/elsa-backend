package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.domain.tyoskentely.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.kayttaja.KayttajaService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoUpdateDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.web.rest.common.TerveyskeskuskoulutusjaksoBaseResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/api/virkailija")
class VirkailijaTerveyskeskuskoulutusjaksoResource(
    userService: UserService,
    kayttajaService: KayttajaService,
    terveyskeskuskoulutusjaksonHyvaksyntaService: TerveyskeskuskoulutusjaksonHyvaksyntaService,
    asiakirjaService: AsiakirjaService
) : TerveyskeskuskoulutusjaksoBaseResource(
    userService,
    kayttajaService,
    terveyskeskuskoulutusjaksonHyvaksyntaService,
    asiakirjaService
) {

    @GetMapping("/terveyskeskuskoulutusjaksot")
    fun getTerveyskeskuskoulutusjaksot(
        principal: Principal?,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): ResponseEntity<Page<TerveyskeskuskoulutusjaksoSimpleDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByVirkailijaUserId(user.id!!, criteria, pageable)
        )
    }

    @GetMapping("/terveyskeskuskoulutusjakso/{id}")
    fun getTerveyskeskuskoulutusjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!).get()
        val yliopistoIds = kayttaja.yliopistot?.map { it.id!! }.orEmpty().toList()
        return withTerveyskeskusExceptionHandling {
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByIdAndYliopistoIdVirkailija(id, yliopistoIds)
                ?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/terveyskeskuskoulutusjakso/tyoskentelyjakso-liite/{id}")
    fun getTerveyskeskuskoulutusjaksoTyoskentelyjaksoLiite(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<ByteArray> {
        val user = userService.getAuthenticatedUser(principal)
        val kayttaja = kayttajaService.findByUserId(user.id!!)
        return buildAsiakirjaDownloadResponse(
            asiakirjaService.findByIdAndTyoskentelyjaksoTyyppi(
                id,
                TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                kayttaja.orElse(null)?.yliopistot?.map { it.id!! }
            )
        )
    }

    @PutMapping("/terveyskeskuskoulutusjakson-hyvaksynta/{id}")
    fun updateTerveyskeskuskoulutusjaksonHyvaksynta(
        @PathVariable id: Long,
        dto: TerveyskeskuskoulutusjaksoUpdateDTO?,
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            terveyskeskuskoulutusjaksonHyvaksyntaService.update(
                user.id!!,
                true,
                id,
                dto?.korjausehdotus,
                dto?.lisatiedotVirkailijalta,
                laillistamispaiva,
                laillistamispaivanLiite
            )
        )
    }
}
