package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    private val adapter = TurkuDynastyArkistointiAdapter(client,metadata,ledger,jacksonObjectMapper())
    private val execution = ArkistointiJobExecution("archive",UUID.randomUUID().toString(),1)
    private val submission = ArkistointiSubmission(execution.jobId,YliopistoEnum.TURUN_YLIOPISTO,CaseType.KOEJAKSO,"{}",emptyList(),execution)

    @Test
    fun `completed steps resume without repeating remote mutations`() {
        whenever(metadata.plan(submission)).thenReturn(TurkuDynastyMetadataMapper.Plan("x","{}","Arviointi",emptyList()))
        whenever(ledger.begin(execution,"CASE")).thenReturn(DynastyStepLedger.Decision.Done("415"))
        whenever(ledger.begin(execution,"TASK")).thenReturn(DynastyStepLedger.Decision.Done(UUID.randomUUID().toString()))
        whenever(ledger.begin(execution,"ARCHIVE_CASE")).thenReturn(DynastyStepLedger.Decision.Done(null))
        assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))
        verify(client).validateConfiguration()
        verifyNoMoreInteractions(client)
        verify(ledger,never()).complete(any(),any(),any(),anyOrNull())
    }

    @Test
    fun `unknown old intent parks delivery without issuing another request`() {
        whenever(metadata.plan(submission)).thenReturn(TurkuDynastyMetadataMapper.Plan("x","{}","Arviointi",emptyList()))
        whenever(ledger.begin(execution,"CASE")).thenReturn(DynastyStepLedger.Decision.Unconfirmed)
        assertThat(adapter.arkistoi(submission)).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
        verify(client).validateConfiguration()
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
