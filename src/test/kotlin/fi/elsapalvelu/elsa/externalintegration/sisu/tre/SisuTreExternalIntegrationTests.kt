package fi.elsapalvelu.elsa.externalintegration.sisu.tre

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("external-integration")
class SisuTreExternalIntegrationTests : SisuTreExternalIntegrationTestBase() {

    @Test
    fun shouldFetchAccessToken() {
        val accessToken = requestAccessToken()

        assertSuccessfulToken(accessToken)
    }

    @Test
    fun shouldFetchAttainmentsForHetu() {
        val response = postId("attainments")

        assertSuccessfulResponse(response)
    }

    @Test
    fun shouldFetchStudyRightsForHetu() {
        val response = postId("study-rights")

        assertSuccessfulResponse(response)
    }
}
