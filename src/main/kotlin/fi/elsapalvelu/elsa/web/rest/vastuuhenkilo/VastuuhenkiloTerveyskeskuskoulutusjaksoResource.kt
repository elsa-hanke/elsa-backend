package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.domain.tyoskentely.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.kayttaja.KayttajaService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksoUpdateDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.web.rest.common.TerveyskeskuskoulutusjaksoBaseResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vastuuhenkilo")
class VastuuhenkiloTerveyskeskuskoulutusjaksoResource(
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
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByVastuuhenkiloUserId(user.id!!, criteria, pageable)
        )
    }

    @GetMapping("/terveyskeskuskoulutusjakso/{id}")
    fun getTerveyskeskuskoulutusjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return withTerveyskeskusExceptionHandling {
            terveyskeskuskoulutusjaksonHyvaksyntaService.findByIdAndVastuuhenkiloUserId(id, user.id!!)
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
        val kayttaja = kayttajaService.findByUserId(user.id!!).orElse(null)
        return buildAsiakirjaDownloadResponse(
            asiakirjaService.findByIdAndTyoskentelyjaksoTyyppiForVastuuhenkilo(
                id,
                TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                kayttaja.yliopistotAndErikoisalat
            )
        )
    }

    @PutMapping("/terveyskeskuskoulutusjakson-hyvaksynta/{id}")
    fun updateTerveyskeskuskoulutusjaksonHyvaksynta(
        @PathVariable id: Long,
        @RequestBody dto: TerveyskeskuskoulutusjaksoUpdateDTO?,
        principal: Principal?
    ): ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        return ResponseEntity.ok(
            terveyskeskuskoulutusjaksonHyvaksyntaService.update(
                user.id!!,
                false,
                id,
                dto?.korjausehdotus,
                dto?.lisatiedotVirkailijalta
            )
        )
    }
}
