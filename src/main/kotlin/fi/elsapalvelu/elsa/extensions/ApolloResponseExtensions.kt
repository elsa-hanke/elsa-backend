package fi.elsapalvelu.elsa.extensions

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloHttpException
import org.slf4j.Logger

private const val GRAPHQL_NO_VALUE_PRESENT_ERROR = "Unexpected Internal Error: No value present"

fun <D : Operation.Data> ApolloResponse<D>.checkErrors(context: String, log: Logger): ApolloResponse<D> {
    // Network / parsing error
    exception?.let { ex ->
        if (ex is ApolloHttpException) {
            val responseBody = try { ex.body?.readUtf8() } catch (_: Exception) { null }
            log.error("$context. HTTP ${ex.statusCode} virhe. Response body: $responseBody", ex)
        } else {
            log.error("$context. Virhe: ${ex.message}", ex)
        }
        throw ex
    }

    // GraphQL-level errors
    if (hasErrors()) {
        val errMsg = errors!!.joinToString("; ") { err ->
            buildString {
                append(err.message)
                err.path?.let { append(", path: $it") }
                err.extensions?.let { append(", extensions: $it") }
            }
        }
        log.error("$context. GraphQL-virheet: $errMsg")
        throw RuntimeException("$context. GraphQL-virheet: $errMsg")
    }

    return this
}
