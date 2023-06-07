package fi.elsapalvelu.elsa.web.rest.errors

import jakarta.servlet.ServletException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException


@ControllerAdvice
class BadRequestExceptionAdvice {

    private val log = LoggerFactory.getLogger(javaClass)
    @ExceptionHandler(BadRequestAlertException::class)
    fun handleBadRequestException(e: BadRequestAlertException): ProblemDetail {
        log.warn(e.message ?: e.defaultMessage, e)
        val body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.defaultMessage)
        body.setProperty("message", "error.${e.errorKey}")
        body.setProperty("params", e.entityName)

        return body
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ProblemDetail {
        log.warn(e.message, e)
        return ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, e.message.orEmpty())
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ProblemDetail {
        log.warn(e.message, e)
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message.orEmpty())
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(e: BindException): ProblemDetail {
        log.warn(e.message, e)
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message.orEmpty())
    }

    @ExceptionHandler(NoSuchMethodError::class)
    fun handleNoSuchMethodError(e: NoSuchMethodError): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message.orEmpty())
    }

    @ExceptionHandler(ServletException::class)
    fun handleServletException(e: ServletException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message.orEmpty())
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.message.orEmpty())
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException): ProblemDetail {
        if (e.statusCode.is4xxClientError) {
            log.warn(e.message, e)
        } else {
            log.error(e.message, e)
        }
        return ProblemDetail.forStatusAndDetail(e.statusCode, e.message.orEmpty())
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ProblemDetail {
        log.error(e.message, e)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.message.orEmpty())
    }
}
