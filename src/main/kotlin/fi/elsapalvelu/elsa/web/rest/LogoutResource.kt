package fi.elsapalvelu.elsa.web.rest

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import javax.servlet.http.HttpServletRequest

@RestController
@Profile("dev", "prod")
class LogoutResource(
    private val relyingPartyRegistrationRepository: RelyingPartyRegistrationRepository
) {

    @GetMapping("/api/local-logout")
    fun localLogout(request: HttpServletRequest): ResponseEntity<Void> {
        request.session.invalidate()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/api/slo-kaytossa")
    fun sloKaytossa(
        request: HttpServletRequest,
        principal: Principal?
    ): ResponseEntity<Boolean> {
        val saml2Authentication = principal as Saml2Authentication
        val saml2AuthenticatedPrincipal =
            saml2Authentication.principal as Saml2AuthenticatedPrincipal
        val relyingPartyRegistration =
            relyingPartyRegistrationRepository.findByRegistrationId(saml2AuthenticatedPrincipal.relyingPartyRegistrationId)
        return ResponseEntity.ok(!relyingPartyRegistration.assertingPartyDetails.singleLogoutServiceLocation.isNullOrBlank())
    }
}
