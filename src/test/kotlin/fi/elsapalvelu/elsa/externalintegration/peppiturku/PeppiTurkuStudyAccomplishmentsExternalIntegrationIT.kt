package fi.elsapalvelu.elsa.externalintegration.peppiturku

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("external-integration")
class PeppiTurkuStudyAccomplishmentsExternalIntegrationIT : PeppiTurkuExternalIntegrationTestBase() {

    @Test
    fun shouldFetchStudyAccomplishmentsForHetu() {
        val response = postHetu(PeppiTurkuEndpoint.STUDY_ACCOMPLISHMENTS)

        assertSuccessfulResponse(response)
    }
}
