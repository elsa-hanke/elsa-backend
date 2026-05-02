package fi.elsapalvelu.elsa.externalintegration.peppi.uef

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("external-integration")
class PeppiUefExternalIntegrationTests : PeppiUefExternalIntegrationTestBase() {

    @Test
    fun shouldFetchStudentForHetu() {
        val response = postHetu(PeppiUefEndpoint.STUDENT)

        assertSuccessfulResponse(response)
    }

    @Test
    fun shouldFetchStudyAccomplishmentsForHetu() {
        val response = postHetu(PeppiUefEndpoint.STUDY_ACCOMPLISHMENTS)

        assertSuccessfulResponse(response)
    }
}
