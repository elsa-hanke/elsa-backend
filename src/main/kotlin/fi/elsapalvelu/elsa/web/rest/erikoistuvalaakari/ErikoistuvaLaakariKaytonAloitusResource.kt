package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.KaytonAloitusDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariKaytonAloitusResource(
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService
) {
    @PutMapping("/kaytonaloitus")
    fun putDataForFirstLogin(
        @Valid @RequestBody kaytonAloitusDTO: KaytonAloitusDTO,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)

        kaytonAloitusDTO.sahkoposti?.let {
            if (userService.existsByEmail(it)) {
                throw BadRequestAlertException(
                    "Sähköpostiosoitteella löytyy jo käyttäjä",
                    "user",
                    "dataillegal.samalla-sahkopostilla-loytyy-jo-toinen-kayttaja"
                )
            }
            userService.updateEmail(it, user.id!!)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)

        val opintooikeudetVoimassa = opintooikeusService.findAllValidByErikoistuvaLaakariKayttajaUserId(user.id!!)
        val opintooikeusId = kaytonAloitusDTO.opintooikeusId
        val shouldSelectOpintooikeusKaytossa = opintooikeudetVoimassa.count() > 1

        if ((opintooikeusId == null && shouldSelectOpintooikeusKaytossa) ||
            (opintooikeusId != null && !shouldSelectOpintooikeusKaytossa)
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        opintooikeusId?.let { id ->
            if (opintooikeudetVoimassa.find { it.id == id } == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST)
            }
            opintooikeusService.setOpintooikeusKaytossa(user.id!!, id)
        }

        return ResponseEntity.ok().build()
    }
}
