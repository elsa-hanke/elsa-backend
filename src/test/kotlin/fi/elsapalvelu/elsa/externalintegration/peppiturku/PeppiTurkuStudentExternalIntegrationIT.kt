package fi.elsapalvelu.elsa.externalintegration.peppiturku

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("external-integration")
class PeppiTurkuStudentExternalIntegrationIT : PeppiTurkuExternalIntegrationTestBase() {

    @Test
    fun shouldFetchStudentForHetu() {
        val response = postHetu(PeppiTurkuEndpoint.STUDENT)

        assertSuccessfulResponse(response)
    }
}
