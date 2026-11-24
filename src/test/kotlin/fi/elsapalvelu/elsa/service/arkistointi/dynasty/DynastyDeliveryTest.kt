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
    fun `retry sends rejected attachment to the TOS phase without recreating case or summary`() {
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
            with(TurkuTosFixture.GRADUATION) { server.enqueueLookup() }
            listOf("1079","1080","").forEach { server.enqueue(MockResponse().setBody(it)) }
            assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))
            assertResumedDeliveryRequests(server, mapper)
        }
    }

    @ParameterizedTest(name = "attachment title only under summary = {0}")
    @ValueSource(booleans = [false, true])
    fun `required TOS document titles must match exactly at the top level`(nestedAttachment: Boolean) {
        MockWebServer().use { server ->
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            val ledger = mock<DynastyStepLedger>()
            val adapter = adapter(server, mapper, ledger)
            val fixture = TurkuTosFixture.GRADUATION
            val phaseId = TurkuTosFixture.PHASE_ID
            server.enqueue(MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE))
            server.enqueue(requireNotNull(fixture.response(fixture.path)))
            val attachment = """{"title":"${TurkuTosFixture.ATTACHMENT_TITLE}"}"""
            val documents = if (nestedAttachment) {
                """{"title":"${fixture.documentTitle}","childs":[$attachment]}"""
            } else {
                """{"title":"Valmistumisen yhteenveto, EL, EHL tai YEK"},$attachment"""
            }
            server.enqueue(MockResponse().setBody(
                """{"fullCasegroupCode":"${fixture.tosClass}","taskRowId":"$phaseId","thsDocuments":[$documents]}"""
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

    private fun assertResumedDeliveryRequests(server: MockWebServer, mapper: ObjectMapper) {
        assertThat(server.requestCount).isEqualTo(11)
        val requests = List(server.requestCount) { server.takeRequest() }
        assertThat(requests.count { it.path!!.startsWith("/api/v1/cases?") }).isEqualTo(1)
        val lookups = requests.filter { it.method == "GET" }
        val phasePath = TurkuTosFixture.GRADUATION.path
        val typesPath = "$phasePath/${TurkuTosFixture.PHASE_ID}/documenttypes"
        assertThat(lookups.map { it.path }).containsExactly(phasePath, typesPath, phasePath, typesPath)
        assertThat(lookups.map { it.getHeader("Authorization") }).containsOnly("Bearer test")
        assertThat(lookups.map { it.getHeader("Organization") }).containsOnly("0")
        assertThat(requests.map { it.path }).noneMatch { it?.endsWith("/tasks") == true }
        val bodies = requests.filter { it.path == "/api/v1/documents" }.map { it.body.readUtf8() }
        assertThat(bodies).hasSize(4)
        assertThat(bodies[0]).contains("filename=\"file0.pdf\"", "UTUvalmistumispaiva", "Confirmed regulation")
        val summaryMetadata = mapper.dynastyMultipartJson(bodies[0], "metaData")
        bodies.drop(1).forEach { body ->
            val options = mapper.dynastyMultipartJson(body, "caseAttachmentOptions")
            val metadata = mapper.dynastyMultipartJson(body, "metaData")
            assertThat(options.fieldNames().asSequence().toList()).containsExactlyInAnyOrder("CaseSelector", "PhaseSelector", "TOSTitle")
            assertThat(options.at("/CaseSelector/Id").longValue()).isEqualTo(415)
            assertThat(options.at("/PhaseSelector/smartAttachOptions/pathSelector/0/Id").asText()).isEqualTo(TurkuTosFixture.PHASE_ID.toString())
            assertThat(options.path("TOSTitle").asText()).isEqualTo(TurkuTosFixture.ATTACHMENT_TITLE)
            assertThat(metadata.fieldNames().asSequence().toList())
                .containsExactlyInAnyOrder("title", "schemaId", "state", "DeliveryType", "languageId", "AD")
            assertThat(metadata.path("languageId").asText()).isEqualTo("fi-FI")
            assertThat(metadata.path("schemaId")).isEqualTo(summaryMetadata.path("schemaId"))
        }
        assertThat(requests.last().path).isEqualTo("/api/v1/cases/415/state/Archived")
    }

}
