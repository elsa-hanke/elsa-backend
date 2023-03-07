package fi.elsapalvelu.elsa.interceptor

import fi.elsapalvelu.elsa.audit.AuditLoggingWrapper
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Component
class GetRequestInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method != "GET") return true
        val context = SecurityContextHolder.getContext()
        if (context.authentication is Saml2Authentication) {
            val authentication = context.authentication as Saml2Authentication
            val principal = authentication.principal as AuthenticatedPrincipal
            AuditLoggingWrapper.info(
                "GET request for ${request.requestURI}, user id: ${principal.name}"
            )
        }
        return true
    }

}
