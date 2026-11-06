package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.verifyNoInteractions

/** Delivery outcomes of a job processed by a real scheduler: success, retries and terminal failures. */
class ArkistointiJobDeliveryIT : ArkistointiJobProcessingItBase() {

    @Test
    fun `retry is delayed and success persists metadata before removing scheduler row`() {
        val jobId = fixture()
        val originalExecution = execution(jobId)
        adapter.behavior = {
            if (adapter.submissions.size == 1) retryable() else ArkistointiResult.Success("external-id")
        }
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(failures(jobId)).isEqualTo(1) }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        assertThat(nextExecutionTime(jobId)).isAfter(Instant.now())
        runUntil(scheduler) { assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY) }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertSucceeded(jobId, "external-id")
        assertThat(adapter.submissions).hasSize(2)
        assertThat(adapter.submissions.map { it.key }).containsOnly(jobId)
        assertThat(adapter.submissions.map { it.jobId }).containsOnly(jobId)
        assertThat(jobId.toString()).isNotEqualTo(storedJob(jobId).key)
        assertThat(adapter.submissions.map { it.payload }).containsOnly("{\"approved\":true}")
        assertThat(adapter.submissions.first().asiakirjat.single().sisalto)
            .containsExactly(*adapter.submissions.last().asiakirjat.single().sisalto)
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        assertThat(adapter.submissions).hasSize(2)
    }

    @Test
    fun `retry limit includes the first delivery`() {
        val jobId = fixture()
        val originalExecution = execution(jobId)
        adapter.behavior = { retryable() }
        val scheduler = scheduler()
        runUntil(scheduler) { assertParked(jobId) }
        assertThat(adapter.submissions).hasSize(3)
        assertThat(storedJob(jobId).errorCode).isEqualTo("ARCHIVE_RETRIES_EXHAUSTED")
        assertThat(storedJob(jobId).lastError).contains("UNAVAILABLE", "Palvelu ei vastaa")
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        assertThat(adapter.submissions).hasSize(3)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 2, 3])
    fun `permanent adapter failure terminates without retry`(previousFailures: Int) {
        val jobId = fixture()
        updateSchedulerRow(jobId, "consecutive_failures = ?", previousFailures)
        adapter.behavior = { ArkistointiResult.PermanentFailure(ArkistointiError.create("REJECTED", "Aineisto hylattiin")) }
        assertTerminal(jobId, "REJECTED")
        assertThat(adapter.submissions).hasSize(1)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NULL", "MAX_ASCII", "MAX_UNICODE", "OPAQUE", "TOO_LONG", "TOO_LONG_UNICODE",
        "EMPTY", "BLANK", "NUL", "HIGH_SURROGATE", "LOW_SURROGATE"])
    fun `external ID contract controls whether success or manual intervention is committed`(kind: String) {
        val externalId = ArkistointiExternalIdTestData.value(kind)
        val valid = kind in setOf("NULL", "MAX_ASCII", "MAX_UNICODE", "OPAQUE")
        adapter.behavior = { ArkistointiResult.Success(externalId) }
        val jobId = fixture()
        val originalExecution = execution(jobId)
        val scheduler = scheduler()
        runUntil(scheduler) { if (valid) assertThat(taskCount(jobId)).isZero() else assertParked(jobId) }
        if (valid) {
            assertSucceeded(jobId, externalId)
        } else {
            val stored = storedJob(jobId)
            assertThat(stored.status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
            assertThat(stored.externalId).isNull()
            assertThat(stored.completedAt).isNull()
            assertThat(stored.errorCode).isEqualTo("ARCHIVE_ADAPTER_RESPONSE_INVALID")
            assertThat(stored.lastError).doesNotContain("private-response")
        }
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        scheduler.triggerCheckForDueExecutions()
        assertThat(adapter.submissions).hasSize(1)
    }

    @Test
    fun `unexpected exceptions are sanitized and retried`() {
        val jobId = fixture()
        adapter.behavior = { throw IllegalStateException("secret personal response body") }
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(failures(jobId)).isEqualTo(1) }
        val stored = storedJob(jobId)
        assertThat(stored.errorCode).isEqualTo("ARCHIVE_PROCESSING_FAILED")
        assertThat(stored.lastError).doesNotContain("secret")
        adapter.behavior = { ArkistointiResult.Success(null) }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
    }

    @Test
    fun `missing adapter terminates with an explicit error`() {
        val jobId = fixture(YliopistoEnum.OULUN_YLIOPISTO)
        assertTerminal(jobId, "ARCHIVE_ADAPTER_MISSING")
        assertThat(adapter.submissions).isEmpty()
    }

    @Test
    fun `unsupported case type terminates without loading documents or sending`() {
        val jobId = fixture(caseType = CaseType.KOEJAKSO)

        assertTerminal(jobId, "ARCHIVE_CASE_TYPE_UNSUPPORTED")

        assertThat(adapter.submissions).isEmpty()
        verifyNoInteractions(loader)
    }

    @Test
    fun `checksum mismatch and invalid payload terminate without invoking adapter`() {
        val corruptJob = fixture(checksum = "0".repeat(64))
        assertTerminal(corruptJob, "ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
        val invalidJob = fixture(payload = "not-json")
        assertTerminal(invalidJob, "ARCHIVE_JOB_INVALID")
        assertThat(adapter.submissions).isEmpty()
    }
}
