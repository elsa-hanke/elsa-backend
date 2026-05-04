package fi.elsapalvelu.elsa.externalintegration.peppi.oulu

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("external-integration")
class PeppiOuluExternalIntegrationTests : PeppiOuluExternalIntegrationTestBase() {

    @Test
    fun shouldFetchStudentForHetu() {
        val response = executeGraphQl(PeppiOuluEndpoint.STUDENT)

        assertSuccessfulResponse(response)
    }

    @Test
    fun shouldFetchStudyAccomplishmentsForHetu() {
        val response = executeGraphQl(PeppiOuluEndpoint.STUDY_ACCOMPLISHMENTS)

        assertSuccessfulResponse(response)
    }
}
