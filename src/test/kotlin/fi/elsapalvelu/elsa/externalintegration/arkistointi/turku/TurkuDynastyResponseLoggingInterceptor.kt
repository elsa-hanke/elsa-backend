package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException

internal class TurkuDynastyResponseLoggingInterceptor(
    settings: DynastyProperties,
    private val mapper: ObjectMapper,
    private val output: (String) -> Unit = { LoggerFactory.getLogger(TurkuDynastyResponseLoggingInterceptor::class.java).info(it) }
) : Interceptor {
    private val tokenUrl = settings.tokenUrl?.toHttpUrlOrNull()
    private val credentials = listOfNotNull(settings.clientId, settings.clientSecret).filter { it.isNotBlank() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val endpoint = "${request.method} ${request.url}"
        val response = try {
            chain.proceed(request)
        } catch (exception: IOException) {
            output("Turku Dynasty $endpoint -> no HTTP response (${exception.javaClass.simpleName})")
            throw exception
        }
        val status = "${response.code} ${response.message}".trimEnd()
        val body = responseDetails(request, response)
        output("Turku Dynasty $endpoint -> HTTP $status; body=$body")
        return response
    }

    private fun responseDetails(request: Request, response: Response): String {
        val tokenResponse = request.url == tokenUrl
        if (tokenResponse && response.isSuccessful) return "<OAuth token response omitted>"

        val body = try {
            response.peekBody(MAX_BODY_BYTES + 1).bytes()
        } catch (exception: IOException) {
            return "<unavailable: ${exception.javaClass.simpleName}>"
        }
        if (body.isEmpty()) return "<empty>"
        if (body.size > MAX_BODY_BYTES) return "<over $MAX_BODY_BYTES bytes>"

        val text = body.toString(response.body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
        val details = if (tokenResponse) oauthErrorDetails(text) else text
        val bearer = request.header("Authorization")?.removePrefix("Bearer ")
        return (credentials + listOfNotNull(bearer)).filter { it.isNotEmpty() }.sortedByDescending { it.length }
            .fold(details) { value, credential ->
                value.replace(credential, "<redacted>")
                    .replace(mapper.writeValueAsString(credential).removeSurrounding("\""), "<redacted>")
            }.replace("\r", "\\r").replace("\n", "\\n")
    }

    private fun oauthErrorDetails(body: String): String {
        val json = try {
            mapper.readTree(body)
        } catch (_: JsonProcessingException) {
            return body
        }
        if (json?.isObject != true) return "<unexpected OAuth error body>"
        val fields = listOf("error", "error_description", "error_uri")
        val details = mapper.createObjectNode()
        fields.forEach { field ->
            json.path(field).takeIf { it.isTextual }?.let { details.put(field, it.textValue()) }
        }
        return details.toString()
    }

    private companion object {
        const val MAX_BODY_BYTES = 4096L
    }
}
