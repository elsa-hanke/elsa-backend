package fi.elsapalvelu.elsa.security

import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class LoginListener : ApplicationListener<InteractiveAuthenticationSuccessEvent> {

    override fun onApplicationEvent(event: InteractiveAuthenticationSuccessEvent) {
        val principal = event.authentication.principal as Saml2AuthenticatedPrincipal
        val requestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        val sourceIpAddress = requestAttributes.request.remoteAddr
        SecurityLoggingWrapper.info("Authentication success event: " +
            "User id: ${principal.name}. Source IP address: $sourceIpAddress")
    }
}
