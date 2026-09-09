package fi.elsapalvelu.elsa.externalintegration.sisu.tre

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

class SisuTreRawTrafficLoggingInterceptor(
    private val output: (String) -> Unit = { System.out.println(it) }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        output(
            buildString {
                appendLine("SISU TRE RAW REQUEST")
                appendLine("${request.method} ${request.url}")
                append(request.body?.let { body ->
                    Buffer().use { buffer ->
                        body.writeTo(buffer)
                        buffer.readString(body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
                    }
                }.orEmpty())
            }
        )

        val response = chain.proceed(request)
        output(
            buildString {
                appendLine("SISU TRE RAW RESPONSE")
                appendLine("HTTP ${response.code} ${response.message}")
                append(response.peekBody(Long.MAX_VALUE).string())
            }
        )
        return response
    }
}

