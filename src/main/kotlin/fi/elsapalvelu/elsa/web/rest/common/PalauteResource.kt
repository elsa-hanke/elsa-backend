package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.service.PalauteService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.PalauteDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/")
class PalauteResource(
    private val userService: UserService,
    private val palauteService: PalauteService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/palaute")
    fun sendPalaute(
        @Valid @RequestBody palauteDTO: PalauteDTO,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        try {
            palauteService.send(palauteDTO, user.id!!)
        } catch (ex: Exception) {
            log.error("Käyttäjän ${user.id!!} antaman palautteen lähettäminen epäonnistui!")
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)
        }
        return ResponseEntity.ok().build()
    }
}

