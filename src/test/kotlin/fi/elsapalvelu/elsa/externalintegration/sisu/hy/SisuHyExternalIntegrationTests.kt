package fi.elsapalvelu.elsa.externalintegration.sisu.hy

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("external-integration")
class SisuHyExternalIntegrationTests : SisuHyExternalIntegrationTestBase() {

    @Test
    fun shouldFetchStudentForHetu() {
        val response = executeGraphQl(SisuHyEndpoint.STUDENT)

        assertSuccessfulResponse(response)
    }

    @Test
    fun shouldFetchQualificationsExport() {
        val response = getQualificationsExport()

        assertSuccessfulResponse(response)
    }

    @Test
    fun shouldFetchStudyAccomplishmentsForHetu() {
        val response = executeGraphQl(SisuHyEndpoint.STUDY_ACCOMPLISHMENTS)

        assertSuccessfulResponse(response)
    }
}
