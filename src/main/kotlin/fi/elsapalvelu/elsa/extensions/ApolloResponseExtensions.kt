package fi.elsapalvelu.elsa.extensions

import fi.elsapalvelu.elsa.required

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error as GraphQLError
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloHttpException
import org.slf4j.Logger

private const val GRAPHQL_NO_VALUE_PRESENT_ERROR = "Unexpected Internal Error: No value present"
private const val PRIVATE_PERSON_BY_PERSONAL_IDENTITY_CODE = "private_person_by_personal_identity_code"
private const val PERSON_NOT_FOUND_ERROR = "Person not found with personal identity code"
private const val HTTP_NOT_FOUND = 404

fun <D : Operation.Data> ApolloResponse<D>.checkErrors(
    context: String,
    log: Logger,
    onAuthenticationResult: (authenticated: Boolean) -> Unit = {}
): ApolloResponse<D> {
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

    val authenticationFailed = hasGraphQlErrorCode("UNAUTHENTICATED")
    onAuthenticationResult(!authenticationFailed)

    // GraphQL-level errors
    if (hasErrors()) {
        val errMsg = errors.required().joinToString("; ") { err ->
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
        if (hasOnlyPersonNotFoundGraphQLErrors()) {
            log.warn("$context. Henkilöä ei löytynyt yliopiston järjestelmästä.")
            return this
        }

        log.error("$context. GraphQL-virheet: $errMsg")
        throw RuntimeException("$context. GraphQL-virheet: $errMsg")
    }

    return this
}

private fun <D : Operation.Data> ApolloResponse<D>.hasOnlyNoValuePresentGraphQLErrors(): Boolean =
    errors?.all { it.message.contains(GRAPHQL_NO_VALUE_PRESENT_ERROR) } == true

private fun <D : Operation.Data> ApolloResponse<D>.hasOnlyPersonNotFoundGraphQLErrors(): Boolean =
    errors?.all { it.isPersonNotFoundGraphQlError() } == true

private fun GraphQLError.isPersonNotFoundGraphQlError(): Boolean {
    val response = extensions?.get("response") as? Map<*, *> ?: return false
    val responseStatus = response["status"]
    val isNotFound = when (responseStatus) {
        is Number -> responseStatus.toInt() == HTTP_NOT_FOUND
        is String -> responseStatus.toIntOrNull() == HTTP_NOT_FOUND
        else -> false
    }

    return isNotFound &&
        path?.lastOrNull() == PRIVATE_PERSON_BY_PERSONAL_IDENTITY_CODE &&
        response["body"].containsText(PERSON_NOT_FOUND_ERROR)
}

private fun Any?.containsText(text: String): Boolean = when (this) {
    is String -> contains(text, ignoreCase = true)
    is Map<*, *> -> values.any { it.containsText(text) }
    is Iterable<*> -> any { it.containsText(text) }
    else -> false
}

private fun <D : Operation.Data> ApolloResponse<D>.hasGraphQlErrorCode(code: String): Boolean =
    errors?.any {
        it.extensions?.get("code")?.toString()?.equals(code, ignoreCase = true) == true
    } == true
