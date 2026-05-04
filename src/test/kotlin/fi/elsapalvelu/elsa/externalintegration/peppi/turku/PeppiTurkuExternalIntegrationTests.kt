package fi.elsapalvelu.elsa.externalintegration.peppi.turku

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("external-integration")
class PeppiTurkuExternalIntegrationTests : PeppiTurkuExternalIntegrationTestBase() {

    @Test
    fun shouldFetchStudentForHetu() {
        val response = postHetu("student")

        assertSuccessfulResponse(response)
    }

    @Test
    fun shouldFetchStudyAccomplishmentsForHetu() {
        val response = postHetu("study_accomplishments")

        assertSuccessfulResponse(response)
    }
}
