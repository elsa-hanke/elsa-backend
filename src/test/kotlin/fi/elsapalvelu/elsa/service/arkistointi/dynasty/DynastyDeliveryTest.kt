package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import java.time.Clock

class DynastyDeliveryTest {
    @Test
    fun `retry resumes rejected attachment and never recreates case or main document`() {
        MockWebServer().use { server ->
            server.start()
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val submission = dynastySubmission(mapper)
            val ledger = resumableLedger(submission.execution)
            val adapter = adapter(server, mapper, ledger)
            server.enqueue(MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE))
            with(TurkuTosFixture.GRADUATION) { server.enqueueLookup() }
            listOf("415", "1078").forEach { server.enqueue(MockResponse().setBody(it)) }
            server.enqueue(MockResponse().setResponseCode(403))
            assertThat(adapter.arkistoi(submission)).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
            listOf("1079","1080","").forEach { server.enqueue(MockResponse().setBody(it)) }
            assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))
            assertResumedDeliveryRequests(server)
        }
    }

    @Test
    fun `a TOS document title with one space prevents all archive mutations`() {
        MockWebServer().use { server ->
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val ledger = mock<DynastyStepLedger>()
            val adapter = adapter(server, mapper, ledger)
            val fixture = TurkuTosFixture.GRADUATION
            val phaseId = TurkuTosFixture.PHASE_ID
            server.enqueue(MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE))
            server.enqueue(requireNotNull(fixture.response(fixture.path)))
            server.enqueue(MockResponse().setBody(
                """{"fullCasegroupCode":"${fixture.tosClass}","taskRowId":"$phaseId","thsDocuments":[
                    {"title":"Valmistumisen yhteenveto, EL, EHL tai YEK"}]}"""
            ))

            val result = adapter.arkistoi(dynastySubmission(mapper)) as ArkistointiResult.PermanentFailure
            assertThat(result.error.code).isEqualTo("DYNASTY_TOS_INVALID")
            verify(ledger, never()).begin(any(), any(), any())
            assertThat(server.requestCount).isEqualTo(3)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [429, 503])
    fun `temporary TOS failure can be retried without recording a create attempt`(status: Int) {
        MockWebServer().use { server ->
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val ledger = mock<DynastyStepLedger>()
            val adapter = adapter(server, mapper, ledger)
            server.enqueue(MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE))
            server.enqueue(MockResponse().setResponseCode(status))

            val result = adapter.arkistoi(dynastySubmission(mapper)) as ArkistointiResult.RetryableFailure
            assertThat(result.error.code).isEqualTo("DYNASTY_TOS_UNAVAILABLE")
            verify(ledger, never()).begin(any(), any(), any())
            assertThat(server.requestCount).isEqualTo(2)
        }
    }

    private fun adapter(server: MockWebServer, mapper: ObjectMapper, ledger: DynastyStepLedger): TurkuDynastyArkistointiAdapter {
        val properties = dynastyTestProperties(server.url("/").newBuilder().host("127.0.0.1").build().toString())
        val clock = Clock.systemUTC()
        return TurkuDynastyArkistointiAdapter(
            DynastyClient(properties.getArkistointi().getTurku().dynasty, mapper, clock),
            TurkuDynastyMetadataMapper(properties, mapper, ArchivePayloadReader(mapper), clock),
            DynastyStepExecutor(ledger), properties
        )
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
        whenever(ledger.existing(eq(execution), any(), any())).thenAnswer { steps[it.getArgument(1)] }
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
        val requests = (1..9).map { server.takeRequest() }
        assertThat(requests.count { it.path!!.startsWith("/api/v1/cases?") }).isEqualTo(1)
        val lookups = requests.filter { it.method == "GET" }
        assertThat(lookups.map { it.path }).containsExactly(
            TurkuTosFixture.GRADUATION.path, "${TurkuTosFixture.GRADUATION.path}/${TurkuTosFixture.PHASE_ID}/documenttypes"
        )
        assertThat(lookups.map { it.getHeader("Authorization") }).containsOnly("Bearer test")
        assertThat(lookups.map { it.getHeader("Organization") }).containsOnly("0")
        assertThat(requests.map { it.path }).noneMatch { it?.endsWith("/tasks") == true }
        val main = requests[4].body.readUtf8()
        assertThat(main).contains(
            "name=\"metaData\"", "name=\"caseAttachmentOptions\"", "name=\"file\"",
            "PhaseSelector", "UTUvalmistumispaiva", "Confirmed regulation"
        )
        assertThat(requests[6].body.readUtf8()).contains("name=\"caseAttachmentOptions\"", "parentDocumentSelector", "1078")
        assertThat(requests.last().path).isEqualTo("/api/v1/cases/415/state/Archived")
    }

}
