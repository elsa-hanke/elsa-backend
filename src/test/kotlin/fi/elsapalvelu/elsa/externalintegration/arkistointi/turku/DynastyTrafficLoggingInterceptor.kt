package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class DynastyTrafficLoggingInterceptor(
    settings: DynastyProperties,
    private val output: (String) -> Unit = { LoggerFactory.getLogger(DynastyTrafficLoggingInterceptor::class.java).info(it) }
) : Interceptor {
    private val credentials = listOfNotNull(settings.clientId, settings.clientSecret).filter { it.isNotBlank() }
        .flatMap { listOf(it, URLEncoder.encode(it, Charsets.UTF_8)) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val secrets = credentials + listOfNotNull(request.header("Authorization")?.removePrefix("Bearer "))
        val target = "${request.method} ${redact(request.url.toString(), secrets)}"
        output("Dynasty request: $target")
        try {
            val response = chain.proceed(request)
            val contentType = redact(response.header("Content-Type").orEmpty(), secrets)
            output("Dynasty response: $target, HTTP ${response.code}, protocol=${response.protocol}, contentType=$contentType")
            if (!response.isSuccessful) {
                logErrorBody(response, secrets)
            }
            return response
        } catch (exception: Exception) {
            val causes = generateSequence(exception as Throwable) { it.cause }.take(MAX_CAUSES)
                .joinToString(" -> ") { "${it.javaClass.name}: ${it.message.orEmpty()}" }
            output("Dynasty request failed: $target, ${redact(causes, secrets)}")
            throw exception
        }
    }

    private fun logErrorBody(response: Response, secrets: List<String>) {
        // Diagnostic reads must not replace the original response or change failure classification.
        try {
            val body = response.peekBody(MAX_ERROR_BYTES + 1).string()
            val truncated = if (body.toByteArray().size > MAX_ERROR_BYTES) " [truncated]" else ""
            output("Dynasty error response body: ${redact(body, secrets)}$truncated")
        } catch (exception: Exception) {
            output("Dynasty error response body unavailable: ${exception.javaClass.name}")
        }
    }

    private fun redact(value: String, secrets: List<String>): String {
        val withoutFields = SENSITIVE_FIELD.replace(value) { "${it.groupValues[1]}\"[REDACTED]\"" }
        return secrets.sortedByDescending { it.length }.fold(withoutFields) { result, secret ->
            result.replace(secret, "[REDACTED]")
        }.replace("\r", "\\r").replace("\n", "\\n")
    }

    private companion object {
        const val MAX_ERROR_BYTES = 16384L
        const val MAX_CAUSES = 8
        val SENSITIVE_FIELD = Regex(
            """(?i)("(?:access_token|refresh_token|id_token|client_secret|client_id)"\s*:\s*)("(?:\\.|[^"\\])*"|[^,}\s]+)"""
        )
    }
}
