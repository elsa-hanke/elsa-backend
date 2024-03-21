package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.service.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.dto.LaillistamispaivaDTO
import fi.elsapalvelu.elsa.service.impl.UserServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/api/yek-koulutettava")
class YekKoulutettavaMuutToiminnotResource(
    private val userService: UserServiceImpl,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService,
) {

    @PutMapping("/laillistamispaiva")
    fun updateYekKoulutettavaLaillistamispaiva(
        @RequestParam(required = false) laillistamispaiva: LocalDate?,
        @RequestParam(required = false) laillistamispaivanLiite: MultipartFile?,
        @RequestParam(required = false) laakarikoulutusSuoritettuSuomiTaiBelgia: Boolean?,
        principal: Principal?
    ) {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.updateLaillistamispaiva(
            user.id!!,
            laillistamispaiva,
            laillistamispaivanLiite?.bytes,
            laillistamispaivanLiite?.originalFilename,
            laillistamispaivanLiite?.contentType
        )

        erikoistuvaLaakariService.updateLaakarikoulutusSuoritettuSuomiTaiBelgia(
            user.id!!, laakarikoulutusSuoritettuSuomiTaiBelgia
        )
    }

    @GetMapping("/laillistamispaiva")
    fun getLaillistamispaiva(
        principal: Principal?
    ): ResponseEntity<LaillistamispaivaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        erikoistuvaLaakariService.getLaillistamispaiva(user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}
