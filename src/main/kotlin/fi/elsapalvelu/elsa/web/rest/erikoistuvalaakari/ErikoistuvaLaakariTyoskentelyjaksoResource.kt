package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import io.github.jhipster.web.util.HeaderUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariTyoskentelyjaksoResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val erikoistuvaLaakariService: ErikoistuvaLaakariService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val tyoskentelypaikka = tyoskentelyjaksoDTO.tyoskentelypaikka
        if (tyoskentelyjaksoDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä.",
                "tyoskentelyjakso",
                "idexists"
            )
        }
        if (tyoskentelypaikka == null || tyoskentelypaikka.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä.",
                "tyoskentelypaikka",
                "idexists"
            )
        }
        if (tyoskentelyjaksoDTO.kaytannonKoulutus == KaytannonKoulutusTyyppi.REUNAKOULUTUS &&
            StringUtils.isEmpty(tyoskentelyjaksoDTO.reunakoulutuksenNimi)
        ) {
            throw BadRequestAlertException(
                "Työskentelyjakso on reunakoulutus, mutta reunakoulutuksen nimi puuttuu.",
                "tyoskentelypaikka",
                "dataillegal"
            )
        }
        val user = userService.getAuthenticatedUser(principal)
        val erikoistuvaLaakari = erikoistuvaLaakariService.findOneByKayttajaUserId(user.id!!)
        if (erikoistuvaLaakari.isPresent) {
            tyoskentelyjaksoDTO.erikoistuvaLaakariId = erikoistuvaLaakari.get().id

            val result = tyoskentelyjaksoService.save(tyoskentelyjaksoDTO)
            return ResponseEntity.created(URI("/api/tyoskentelyjaksot/${result.id}"))
                .headers(
                    HeaderUtil.createEntityCreationAlert(
                        applicationName,
                        true,
                        "tyoskentelyjakso",
                        result.id.toString()
                    )
                )
                .body(result)
        } else {
            throw BadRequestAlertException(
                "Uuden tyoskentelyjakson voi tehdä vain erikoistuva lääkäri.",
                "tyoskentelyjakso",
                "dataillegal"
            )
        }
    }
}
