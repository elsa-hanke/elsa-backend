package fi.elsapalvelu.elsa.externalintegration.sisu.hy

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

class SisuHyRawTrafficLoggingInterceptor(
    private val output: (String) -> Unit = { System.out.println(it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = request.body?.let { body ->
            Buffer().use { buffer ->
                body.writeTo(buffer)
                buffer.readString(body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
            }
        }.orEmpty()

        output(
            buildString {
                appendLine("SISU HY RAW REQUEST")
                appendLine("${request.method} ${request.url}")
                appendLine("Headers:")
                request.headers.forEach { (name, value) ->
                    val masked = if (name.equals("X-Api-Key", ignoreCase = true) ||
                        name.equals("Authorization", ignoreCase = true)
                    ) {
                        "<masked>"
                    } else {
                        value
                    }
                    appendLine("  $name: $masked")
                }
                if (requestBody.isNotBlank()) {
                    appendLine("Body:")
                    appendLine(requestBody)
                }
            }
        )

        val response = chain.proceed(request)
        val responseBody = response.peekBody(Long.MAX_VALUE).string()

        output(
            buildString {
                appendLine("SISU HY RAW RESPONSE")
                appendLine("HTTP ${response.code} ${response.message}")
                appendLine("Headers:")
                response.headers.forEach { (name, value) ->
                    appendLine("  $name: $value")
                }
                if (responseBody.isNotBlank()) {
                    appendLine("Body:")
                    appendLine(responseBody)
                }
            }
        )
        return response
    }
}
