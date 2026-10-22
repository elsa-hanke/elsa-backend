package fi.elsapalvelu.elsa.service.arkistointi.job.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArkistointiSubmissionContractTest {
    @Test
    fun `delivery contract carries the saved UUID separately from the creation business key`() {
        val job = ArkistointiJob(id = UUID.randomUUID(), key = "valmistuminen:TAMPEREEN_YLIOPISTO:123")
        val request = CreateArkistointiJobRequest(
            YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", requireNotNull(job.key), emptyList()
        )
        val submission = ArkistointiSubmission(
            requireNotNull(job.id), request.university, request.caseType, request.payload, emptyList()
        )

        assertThat(request.key).isEqualTo(job.key)
        assertThat(submission.key).isEqualTo(job.id)
        assertThat(submission.copy().key).isEqualTo(submission.key)
        assertThat(submission.key.toString()).isNotEqualTo(request.key)
    }

    @Test
    fun `job and delivery UUID serialize as the same canonical string without payload in diagnostic text`() {
        val key = UUID.fromString("6a703cf8-9942-48a6-8321-3ed21e577e84")
        val submission = ArkistointiSubmission(
            key, YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN, "{\"private\":true}", emptyList()
        )
        val mapper = ObjectMapper()
        val json = mapper.readTree(mapper.writeValueAsString(submission))

        assertThat(json["key"].asText()).isEqualTo(key.toString())
        assertThat(json["jobId"].asText()).isEqualTo(key.toString())
        assertThat(submission.toString()).doesNotContain("private")
    }

    @Test
    fun `delivery key always follows job ID instead of retaining another identity`() {
        val submission = ArkistointiSubmission(
            UUID.randomUUID(), YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", emptyList()
        )
        val other = submission.copy(jobId = UUID.randomUUID())

        assertThat(other.key).isEqualTo(other.jobId).isNotEqualTo(submission.key)
    }
}
