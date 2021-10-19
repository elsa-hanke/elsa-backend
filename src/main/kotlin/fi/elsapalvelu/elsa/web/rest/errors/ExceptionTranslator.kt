package fi.elsapalvelu.elsa.web.rest.errors

import fi.elsapalvelu.elsa.security.SecurityLoggingWrapper
import io.github.jhipster.config.JHipsterConstants
import org.apache.commons.lang3.StringUtils
import org.springframework.core.env.Environment
import org.springframework.dao.ConcurrencyFailureException
import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.NativeWebRequest
import org.zalando.problem.*
import org.zalando.problem.spring.web.advice.ProblemHandling
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait
import org.zalando.problem.violations.ConstraintViolationProblem
import java.net.URI
import javax.servlet.http.HttpServletRequest

private const val FIELD_ERRORS_KEY = "fieldErrors"
private const val MESSAGE_KEY = "message"
private const val PATH_KEY = "path"
private const val VIOLATIONS_KEY = "violations"

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 */
@ControllerAdvice
class ExceptionTranslator(
    private val env: Environment
) : ProblemHandling, SecurityAdviceTrait {

    /**
     * Post-process the Problem payload to add the message key for the front-end if needed.
     */
    override fun process(entity: ResponseEntity<Problem>?, request: NativeWebRequest?): ResponseEntity<Problem>? {
        if (entity == null) {
            return null
        }
        val problem = entity.body
        if (!(problem is ConstraintViolationProblem || problem is DefaultProblem)) {
            return entity
        }
        val nativeRequest = request?.getNativeRequest(HttpServletRequest::class.java)
        val requestUri = if (nativeRequest != null) nativeRequest.requestURI else StringUtils.EMPTY

        val builder = Problem.builder()
            .withType(if (Problem.DEFAULT_TYPE == problem.type) DEFAULT_TYPE else problem.type)
            .withStatus(problem.status)
            .withTitle(problem.title)
            .with(PATH_KEY, requestUri)

        if (problem is ConstraintViolationProblem) {
            builder
                .with(VIOLATIONS_KEY, problem.violations)
                .with(MESSAGE_KEY, ERR_VALIDATION)
        } else {
            builder
                .withCause((problem as DefaultProblem).cause)
                .withDetail(problem.detail)
                .withInstance(problem.instance)
            problem.parameters.forEach { (key, value) -> builder.with(key, value) }
            if (!problem.parameters.containsKey(MESSAGE_KEY) && problem.status != null) {
                builder.with(MESSAGE_KEY, "error.http." + problem.status!!.statusCode)
            }
        }
        return ResponseEntity(builder.build(), entity.headers, entity.statusCode)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: NativeWebRequest
    ): ResponseEntity<Problem>? {
        val result = ex.bindingResult
        val fieldErrors = result.fieldErrors.map {
            FieldErrorVM(
                it.objectName.replaceFirst(Regex("DTO$"), ""),
                it.field,
                it.code
            )
        }

        val problem = Problem.builder()
            .withType(CONSTRAINT_VIOLATION_TYPE)
            .withTitle("Method argument not valid")
            .withStatus(defaultConstraintViolationStatus())
            .with(MESSAGE_KEY, ERR_VALIDATION)
            .with(FIELD_ERRORS_KEY, fieldErrors)
            .build()
        return create(ex, problem, request)
    }

    @ExceptionHandler
    fun handleBadRequestAlertException(
        ex: BadRequestAlertException,
        request: NativeWebRequest
    ): ResponseEntity<Problem>? =
        create(
            ex,
            request
        )

    @ExceptionHandler
    fun handleConcurrencyFailure(
        ex: ConcurrencyFailureException,
        request: NativeWebRequest
    ): ResponseEntity<Problem>? {
        val problem = Problem.builder()
            .withStatus(Status.CONFLICT)
            .with(MESSAGE_KEY, ERR_CONCURRENCY_FAILURE)
            .build()
        return create(ex, problem, request)
    }

    override fun handleAccessDenied(
        e: AccessDeniedException?,
        request: NativeWebRequest?
    ): ResponseEntity<Problem> {
        SecurityLoggingWrapper.warn(
            "Access denied for " +
                "user: ${request.let { it?.userPrincipal?.name }}, " +
                "method: ${request.let { (it?.nativeRequest as HttpServletRequest).method }}, " +
                "path: ${request.let { (it?.nativeRequest as HttpServletRequest).requestURI }}, " +
                "ip: ${request.let { (it?.nativeRequest as HttpServletRequest) }.getHeader("X-Forwarded-For")}"
        )
        return super.handleAccessDenied(e, request)
    }

    override fun prepare(throwable: Throwable, status: StatusType, type: URI): ProblemBuilder {
        val activeProfiles = env.activeProfiles
        var detail = throwable.message
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)) {
            detail = when (throwable) {
                is HttpMessageConversionException -> "Unable to convert http message"
                is DataAccessException -> "Failure during data access"
                else -> {
                    if (containsPackageName(throwable.message)) {
                        "Unexpected runtime exception"
                    } else {
                        throwable.message
                    }
                }
            }
        }
        return Problem.builder()
            .withType(type)
            .withTitle(status.reasonPhrase)
            .withStatus(status)
            .withDetail(detail)
            .withCause(throwable.cause.takeIf { isCausalChainsEnabled }?.let { toProblem(it) })
    }

    private fun containsPackageName(message: String?) =
        listOf("org.", "java.", "net.", "javax.", "com.", "io.", "de.", "fi.elsapalvelu.elsa").any { it == message }
}
