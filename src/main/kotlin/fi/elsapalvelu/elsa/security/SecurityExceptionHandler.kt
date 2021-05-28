package fi.elsapalvelu.elsa.security

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.zalando.problem.Problem
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class SecurityExceptionHandler : SecurityAdviceTrait {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handleAccessDenied(
        e: AccessDeniedException?,
        request: NativeWebRequest?
    ): ResponseEntity<Problem> {
        log.warn(
            "Access denied for " +
                "user: ${request.let { it?.userPrincipal?.name }}, " +
                "method: ${request.let { (it?.nativeRequest as HttpServletRequest).method }}, " +
                "path: ${request.let { (it?.nativeRequest as HttpServletRequest).requestURI }}, " +
                "ip: ${request.let { (it?.nativeRequest as HttpServletRequest) }.remoteAddr}"
        )
        return super.handleAccessDenied(e, request)
    }
}
