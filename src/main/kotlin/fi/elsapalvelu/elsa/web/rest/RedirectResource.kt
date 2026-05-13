package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.config.LoginException
import org.springframework.core.env.Environment
import org.springframework.security.saml2.core.Saml2ErrorCodes
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.security.web.WebAttributes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.view.RedirectView
import tech.jhipster.config.JHipsterConstants
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory

@RestController
class RedirectResource(private val env: Environment) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/")
    fun redirect2FrontendView(request: HttpServletRequest): RedirectView? {
        return RedirectView(getRedirectUrl(request))
    }

    @GetMapping("/kirjaudu")
    fun redirect2LoginView(request: HttpServletRequest): RedirectView? {
        if (request.session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) == null) {
            return RedirectView(getRedirectUrl(request) + "kirjautuminen")
        }
        val exception: Saml2AuthenticationException =
            request.session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) as Saml2AuthenticationException
        var exceptionMessage = LoginException.TUNTEMATON.name

        if (exception.message in (LoginException.entries.map { it.name })) {
            exceptionMessage = exception.message!!
        } else {
            val errorCode = exception.saml2Error.errorCode

            when {
                // IdP-side error (e.g. user cancelled, IdP session expired, IdP internal error).
                // Nothing we can do — log quietly to avoid noise.
                errorCode == Saml2ErrorCodes.INVALID_RESPONSE &&
                    exception.message?.contains("Responder") == true -> {
                    val sourceIpAddress = getSourceIpAddress()
                    log.info("IdP returned Responder status, source IP: $sourceIpAddress, error: $exception")
                }

                // Relying party not found — expected for unregistered SPs, no need to log.
                errorCode == Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND -> Unit

                // Genuinely unexpected — keep visible but warn rather than error
                // since the root cause is still outside our system.
                else -> {
                    val sourceIpAddress = getSourceIpAddress()
                    log.error("Unhandled authentication exception: $exception, source IP: $sourceIpAddress")
                }
            }
        }

        return RedirectView(getRedirectUrl(request) + "kirjautuminen?virhe=" + exceptionMessage)
    }

    private fun getSourceIpAddress(): String {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        return requestAttributes.request.getHeader("X-Forwarded-For")
            ?: requestAttributes.request.remoteAddr
    }

    private fun getRedirectUrl(request: HttpServletRequest): String {
        val activeProfiles = env.activeProfiles
        return if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)) {
            "http://localhost:9060/"
        } else {
            val url = request.requestURL.toString()
            url.replace("api.", "").replace(request.requestURI, "/")
        }
    }
}
