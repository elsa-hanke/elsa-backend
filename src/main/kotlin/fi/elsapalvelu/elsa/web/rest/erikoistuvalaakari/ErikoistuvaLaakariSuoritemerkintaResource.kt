package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.service.OppimistavoitteenKategoriaService
import fi.elsapalvelu.elsa.service.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.service.UserService
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaFormDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariSuoritemerkintaResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val oppimistavoitteenKategoriaService: OppimistavoitteenKategoriaService
) {

    @GetMapping("/suoritemerkinta-lomake")
    fun getSuoritemerkintaLomake(
        principal: Principal?
    ): ResponseEntity<SuoritemerkintaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val id = user.id!!
        val form = SuoritemerkintaFormDTO()
        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(id).toMutableSet()
        form.oppimistavoitteenKategoriat = oppimistavoitteenKategoriaService.findAll().toMutableSet()

        return ResponseEntity.ok(form)
    }
}
