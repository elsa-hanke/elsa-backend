package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class DynastyDeliveryTest {
    @Test
    fun `retry resumes rejected attachment and never recreates case task or main document`() {
        MockWebServer().use { server ->
            server.start()
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val properties = dynastyTestProperties(server.url("/").newBuilder().host("127.0.0.1").build().toString())
            val submission = dynastySubmission(mapper)
            val ledger = resumableLedger(submission.execution)
            val adapter = TurkuDynastyArkistointiAdapter(
                DynastyClient(properties.getArkistointi().getTurku().dynasty,mapper),
                TurkuDynastyMetadataMapper(properties,mapper,ArchivePayloadReader(mapper)),
                DynastyStepExecutor(ledger, properties)
            )
            listOf("""{"access_token":"token","expires_in":900}""","415","\"${UUID.randomUUID()}\"","1078").forEach { server.enqueue(MockResponse().setBody(it)) }
            server.enqueue(MockResponse().setResponseCode(403))
            assertThat(adapter.arkistoi(submission)).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
            listOf("1079","1080","").forEach { server.enqueue(MockResponse().setBody(it)) }
            assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))
            assertResumedDeliveryRequests(server)
        }
    }

    private fun resumableLedger(execution: ArkistointiJobExecution): DynastyStepLedger {
        val ledger = mock<DynastyStepLedger>()
        val steps = mutableMapOf<DynastyStepKey<*>, DynastyStepLedger.Decision>()
        whenever(ledger.begin(eq(execution), any(), any())).thenAnswer {
            val key = it.getArgument<DynastyStepKey<*>>(1)
            steps[key] ?: DynastyStepLedger.Decision.Proceed.also {
                steps[key] = DynastyStepLedger.Decision.Unconfirmed
            }
        }
        whenever(ledger.complete(any(), any(), any(), anyOrNull())).thenAnswer {
            steps[it.getArgument(1)] = DynastyStepLedger.Decision.Done(it.getArgument(3))
            true
        }
        doAnswer {
            steps.remove(it.getArgument<DynastyStepKey<*>>(1))
            null
        }.whenever(ledger).abandon(any(), any(), any())
        return ledger
    }

    private fun assertResumedDeliveryRequests(server: MockWebServer) {
        val requests = (1..8).map { server.takeRequest() }
        assertThat(requests.count { it.path!!.startsWith("/api/v1/cases?") }).isEqualTo(1)
        val main = requests[3].body.readUtf8()
        assertThat(main).contains("MetaData","CaseAttachmentOptions","File","PhaseSelector","UTUvalmistumispaiva","Confirmed regulation")
        assertThat(requests[5].body.readUtf8()).contains("parentDocumentSelector","1078")
        assertThat(requests.last().path).isEqualTo("/api/v1/cases/415/state/Archived")
    }

}
