package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class TurkuDynastyArkistointiAdapterTest {
    private val client = mock<DynastyClient>()
    private val metadata = mock<TurkuDynastyMetadataMapper>()
    private val ledger = mock<DynastyStepLedger>()
    private val adapter = TurkuDynastyArkistointiAdapter(client,metadata,DynastyStepExecutor(ledger))
    private val execution = ArkistointiJobExecution("archive",UUID.randomUUID().toString(),1)
    private val submission = ArkistointiSubmission(execution.jobId,YliopistoEnum.TURUN_YLIOPISTO,CaseType.KOEJAKSO,"{}",emptyList(),execution)
    private val plan = TurkuDynastyMetadataMapper.Plan(
        "x", DynastyCaseRequest(DynastyCaseModel("schema", "Test", DynastyAdditionalData())), "Arviointi", emptyList()
    )

    @Test
    fun `completed steps resume without repeating remote mutations`() {
        whenever(client.inProgressGrace).thenReturn(java.time.Duration.ofMinutes(5))
        whenever(metadata.plan(submission)).thenReturn(plan)
        whenever(ledger.begin(execution,DynastyStepKey.Case,java.time.Duration.ofMinutes(5))).thenReturn(DynastyStepLedger.Decision.Done("415"))
        whenever(ledger.begin(execution,DynastyStepKey.Task,java.time.Duration.ofMinutes(5))).thenReturn(DynastyStepLedger.Decision.Done(UUID.randomUUID().toString()))
        whenever(ledger.begin(execution,DynastyStepKey.ArchiveCase,java.time.Duration.ofMinutes(5))).thenReturn(DynastyStepLedger.Decision.Done(null))
        assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))
        verify(client).validateConfiguration()
        verify(client, times(3)).inProgressGrace
        verifyNoMoreInteractions(client)
        verify(ledger,never()).complete(any(),any(),any(),anyOrNull())
    }

    @Test
    fun `unknown old intent parks delivery without issuing another request`() {
        whenever(client.inProgressGrace).thenReturn(java.time.Duration.ofMinutes(5))
        whenever(metadata.plan(submission)).thenReturn(plan)
        whenever(ledger.begin(execution,DynastyStepKey.Case,java.time.Duration.ofMinutes(5))).thenReturn(DynastyStepLedger.Decision.Unconfirmed)
        assertThat(adapter.arkistoi(submission)).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
        verify(client).validateConfiguration()
        verify(client).inProgressGrace
        verifyNoMoreInteractions(client)
    }

    @Test
    fun `mapping failure creates neither intent nor external object`() {
        whenever(metadata.plan(submission)).thenThrow(IllegalArgumentException("private metadata"))
        val result = adapter.arkistoi(submission) as ArkistointiResult.PermanentFailure
        assertThat(result.error.code).isEqualTo("DYNASTY_PAYLOAD_INVALID")
        verifyNoInteractions(ledger)
        verify(client).validateConfiguration()
        verifyNoMoreInteractions(client)
    }
}
