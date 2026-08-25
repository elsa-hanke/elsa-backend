package fi.elsapalvelu.elsa.extensions

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.benasher44.uuid.uuid4
import fi.elsapalvelu.elsa.OpintotietodataSisuHyQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.slf4j.Logger

class ApolloResponseExtensionsTest {

    private val log = mock<Logger>()

    @Test
    fun `person not found is warned and returned without throwing`() {
        val response = responseWithErrors(
            Error.Builder("404: Not Found")
                .path(listOf("private_person_by_personal_identity_code"))
                .putExtension(
                    "response",
                    mapOf(
                        "status" to 404,
                        "body" to mapOf(
                            "message" to "Person not found with personal identity code"
                        )
                    )
                )
                .build()
        )

        val result = response.checkErrors("Opintotietoja ei saatu haettua", log)

        assertThat(result).isSameAs(response)
        verify(log).warn(
            "Opintotietoja ei saatu haettua. Henkilöä ei löytynyt yliopiston järjestelmästä."
        )
        verify(log, never()).error(any<String>())
    }

    @Test
    fun `other not found error is logged as error and thrown`() {
        val response = responseWithErrors(
            Error.Builder("404: Not Found")
                .path(listOf("another_field"))
                .putExtension(
                    "response",
                    mapOf(
                        "status" to 404,
                        "body" to mapOf(
                            "message" to "Person not found with personal identity code"
                        )
                    )
                )
                .build()
        )

        assertThrows<RuntimeException> {
            response.checkErrors("Opintotietoja ei saatu haettua", log)
        }

        verify(log).error(any<String>())
        verify(log, never()).warn(any<String>())
    }

    private fun responseWithErrors(vararg errors: Error) =
        ApolloResponse.Builder(OpintotietodataSisuHyQuery("hetu"), uuid4())
            .errors(errors.toList())
            .build()
}
