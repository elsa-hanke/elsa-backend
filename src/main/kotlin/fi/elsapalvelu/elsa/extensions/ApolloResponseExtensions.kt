package fi.elsapalvelu.elsa.extensions

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import org.slf4j.Logger

private const val GRAPHQL_NO_VALUE_PRESENT_ERROR = "Unexpected Internal Error: No value present"

fun <D : Operation.Data> ApolloResponse<D>.checkErrors(context: String, log: Logger): ApolloResponse<D> {
    // Network / parsing error
    exception?.let { ex ->
        log.error("$context. Virhe: ${ex.message}", ex)
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
        if (hasOnlyNoValuePresentGraphQLErrors()) {
            log.warn("$context. GraphQL-virheet: $errMsg")
            return this
        }

        log.error("$context. GraphQL-virheet: $errMsg")
        throw RuntimeException("$context. GraphQL-virheet: $errMsg")
    }

    return this
}

private fun <D : Operation.Data> ApolloResponse<D>.hasOnlyNoValuePresentGraphQLErrors(): Boolean =
    errors?.all { it.message.contains(GRAPHQL_NO_VALUE_PRESENT_ERROR) } == true
