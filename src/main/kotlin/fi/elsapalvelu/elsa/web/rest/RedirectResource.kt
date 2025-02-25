package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.config.LoginException
import org.springframework.core.env.Environment
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.security.web.WebAttributes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import tech.jhipster.config.JHipsterConstants
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.saml2.core.Saml2ErrorCodes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

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

            // Lokitetaan vain suomi.fi/haka virheet
            if (errorCode != Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND) {
                val requestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
                val sourceIpAddress =
                    requestAttributes.request.getHeader("X-Forwarded-For") ?: requestAttributes.request.remoteAddr
                log.error("Unknown authentication exception: $exception, source IP address: $sourceIpAddress")
            }
        }
        return RedirectView(getRedirectUrl(request) + "kirjautuminen?virhe=" + exceptionMessage)
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
