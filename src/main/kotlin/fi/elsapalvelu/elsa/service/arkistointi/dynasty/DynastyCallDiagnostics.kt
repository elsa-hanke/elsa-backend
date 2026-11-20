package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import java.io.IOException
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal inline fun <T> diagnoseDynastyCall(operation: String, call: DynastyCallDiagnostics.() -> T): T {
    val diagnostics = DynastyCallDiagnostics(operation)
    return try {
        diagnostics.call()
    } catch (failure: DynastyFailure) {
        diagnostics.logFailure(failure)
        throw failure
    }
}

internal class DynastyCallDiagnostics(private val operation: String) {
    private var httpStatus: Int? = null
    private var requestId: String? = null
    private var exceptionType: String? = null
    private var errorResponseBody: String? = null

    fun recordResponse(response: Response) {
        httpStatus = response.code
        requestId = sequenceOf("X-Request-ID", "X-Correlation-ID")
            .mapNotNull { response.header(it) }
            .firstOrNull(requestIdFormat::matches)
        if (!response.isSuccessful) {
            try {
                errorResponseBody = response.body?.string()
            } catch (exception: IOException) {
                recordException(exception)
            }
        }
    }

    fun recordException(exception: Exception) {
        exceptionType = exception.javaClass.simpleName
    }

    fun logUnauthorizedRetry() = logFailure("unauthorized_token_refresh")

    fun logFailure(failure: DynastyFailure) =
        logFailure((failure.diagnosticReason?.name ?: failure.category.name).lowercase())

    private fun logFailure(reason: String) {
        // The production log appenders already expose MDC as JSON fields.
        val previous = MDC.getCopyOfContextMap()
        try {
            MDC.put("dynastyOperation", operation)
            MDC.put("dynastyReason", reason)
            httpStatus?.let { MDC.put("httpStatus", it.toString()) }
            requestId?.let { MDC.put("dynastyRequestId", it) }
            exceptionType?.let { MDC.put("exceptionType", it) }
            val responseDetails = errorResponseBody?.let { " responseBody=$it" }.orEmpty()
            log.error("Dynasty call failed [operation={}, reason={}, httpStatus={}]{}", operation, reason, httpStatus, responseDetails)
        } finally {
            if (previous == null) MDC.clear() else MDC.setContextMap(previous)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(DynastyCallDiagnostics::class.java)
        // These headers are optional. Accept only UUID-shaped correlation values when present.
        val requestIdFormat = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    }
}
