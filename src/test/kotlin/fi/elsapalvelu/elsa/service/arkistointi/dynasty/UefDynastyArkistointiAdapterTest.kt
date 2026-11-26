package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyMetadataMapper
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

class UefDynastyArkistointiAdapterTest {
    private val university = YliopistoEnum.ITA_SUOMEN_YLIOPISTO
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val reader = ArchivePayloadReader(mapper)

    @ParameterizedTest
    @CsvSource(
        "EL, Erikoislääkärikoulutus",
        "EHL, Erikoishammaslääkärikoulutus",
        "YEK, Yleislääketieteen erityiskoulutus"
    )
    fun `graduation uses UEF metadata for each programme and document`(prefix: String, programme: String) {
        val properties = dynastyTestProperties("https://dynasty.example", university)
        val submission = dynastySubmission(
            mapper, specialtyType = specialtyType(prefix), yek = prefix == "YEK", university = university
        )
        val settings = properties.getArkistointi().getUef().dynasty
        val plan = DynastyMetadataMapper(DynastyArchiveProfile.UEF, settings, mapper, reader, Clock.systemUTC()).plan(submission)
        val case = mapper.valueToTree<JsonNode>(plan.caseRequest)["caseModel"]
        val (summary, attachments, license) = plan.documents.map { mapper.valueToTree<JsonNode>(it) }

        assertThat(case["title"].asText()).isEqualTo("$prefix Valmistumisen yhteenveto Maija Meikäläinen 31.10.2026")
        assertThat(case["schemaid"].asText()).isEqualTo("case-schema")
        assertThat(case["manner"].asText()).isEqualTo("M")
        assertThat(case["AD"].fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "UEFetunimi", "UEFsukunimi", "UEFsyntymaaika", "UEFtiedekunta", "UEFkoulutusohjelma", "UEFpaaaine"
        )
        assertThat(case["AD"]["UEFkoulutusohjelma"].asText()).isEqualTo(programme)
        assertThat(case["AD"]["UEFtiedekunta"].asText()).isEqualTo("Terveystieteiden tiedekunta")
        assertThat(case.has("retentionPeriodLength")).isFalse()
        assertThat(case["retentionPeriod"]["End"].asText()).isEqualTo("9999-12-31T00:00:00")

        assertThat(summary["title"]).isEqualTo(case["title"])
        assertThat(summary["AD"]["UEFalkamispaiva"].asText()).isEqualTo("2020-01-01")
        assertThat(summary["AD"]["UEFvalmistumispaiva"].asText()).isEqualTo("2026-10-31")
        assertThat(summary["AD"]["UEFtutkintoasetus"].asText()).isEqualTo("Confirmed regulation")
        assertThat(summary["AD"]["UEFlaillistamispaiva"].asText()).isEqualTo("2019-01-01")
        assertThat(summary["AD"].has("ExtElsaTunniste")).isFalse()
        assertThat(listOf(summary, attachments, license)).allSatisfy { assertThat(it.has("languageId")).isFalse() }
        assertThat(attachments["title"].asText()).isEqualTo("Valmistumisen yhteenvedon liitteet Maija Meikäläinen 31.10.2026")
        assertThat(license["title"].asText()).isEqualTo("Laillistustodistus Maija Meikäläinen")
        assertThat(attachments.has("AD")).isFalse()
        assertThat(license["AD"]).isEqualTo(mapper.readTree("""{"UEFlaillistamispaiva":"2019-01-01"}"""))
    }

    @ParameterizedTest
    @CsvSource("EL,LAAKETIEDE", "EHL,HAMMASLAAKETIEDE", "YEK,LAAKETIEDE")
    fun `koejakso maps each programme to a partially secret permanent assessment`(prefix: String, type: ErikoisalaTyyppi) {
        val properties = dynastyTestProperties("https://dynasty.example", university)
        val submission = dynastySubmission(mapper, CaseType.KOEJAKSO, type, yek = prefix == "YEK", university = university)
        val settings = properties.getArkistointi().getUef().dynasty
        val plan = DynastyMetadataMapper(DynastyArchiveProfile.UEF, settings, mapper, reader, Clock.systemUTC()).plan(submission)
        val case = mapper.valueToTree<JsonNode>(plan.caseRequest)["caseModel"]
        val document = mapper.valueToTree<JsonNode>(plan.documents.single())
        assertThat(case["title"].asText()).isEqualTo("$prefix Koejakson vastuuhenkilön arvio Maija Meikäläinen 31.10.2026")
        assertThat(case["publicityClassId"].asText()).isEqualTo("J")
        assertThat(plan.tosClass).isEqualTo("03.11")
        assertThat(plan.task).isEqualTo("ELSA Koejaksojen yhteenvedot")
        assertThat(document["title"]).isEqualTo(case["title"])
        assertThat(document.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "title", "schemaId", "AD", "DeliveryType", "state", "SecrecyPeriod"
        )
        val start = java.time.LocalDateTime.parse(document["SecrecyPeriod"]["Start"].asText())
        assertThat(java.time.LocalDateTime.parse(document["SecrecyPeriod"]["End"].asText())).isEqualTo(start.plusYears(100))
        assertThat(document["AD"]["UEFhyvaksymispaiva"].asText()).isEqualTo("2026-10-31")
        assertThat(document["AD"].has("UEFvalmistumispaiva")).isFalse()
    }

    @Test
    fun `graduation sends all documents to the same phase before archiving the case`() {
        MockWebServer().use { server ->
            val baseUrl = server.url("/").newBuilder().host("127.0.0.1").build().toString()
            val properties = dynastyTestProperties(baseUrl, university)
            val settings = properties.getArkistointi().getUef().dynasty.apply { inProgressGrace = Duration.ofMinutes(9) }
            val ledger = mock<DynastyStepLedger>()
            whenever(ledger.begin(any(), any(), any())).thenReturn(DynastyStepLedger.Decision.Proceed)
            whenever(ledger.complete(any(), any(), any(), anyOrNull())).thenReturn(true)
            val adapter = DynastyArkistointiAdapter(
                DynastyArchiveProfile.UEF,
                DynastyClient(settings, mapper, Clock.systemUTC()),
                DynastyMetadataMapper(DynastyArchiveProfile.UEF, settings, mapper, reader, Clock.systemUTC()),
                DynastyStepExecutor(ledger), settings
            )
            val submission = dynastySubmission(mapper, university = university)
            val phaseId = UUID.randomUUID()
            DynastyTosFixture.UEF_GRADUATION.enqueueSuccess(server, listOf(1078L, 1079L, 1080L), phaseId)

            assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))

            val requests = List(8) { requireNotNull(server.takeRequest(2, TimeUnit.SECONDS)) }
            assertThat(requests.map { it.path }).containsExactly(
                "/token", "/api/v1/cases/phases/03.11", "/api/v1/cases/phases/03.11/$phaseId/documenttypes",
                "/api/v1/cases?tosClass=03.11",
                "/api/v1/documents", "/api/v1/documents", "/api/v1/documents", "/api/v1/cases/415/state/Archived"
            )
            assertParallelDocuments(requests.subList(4, 7), phaseId)
            verify(ledger, never()).begin(any(), eq(DynastyStepKey.Task), any())
            verify(ledger).begin(submission.execution, DynastyStepKey.Case, Duration.ofMinutes(9))
        }
    }

    private fun assertParallelDocuments(requests: List<RecordedRequest>, phaseId: UUID) {
        val documents = requests.mapIndexed { index, request ->
            val body = request.body.readUtf8()
            assertThat(body).contains("name=\"File\"; filename=\"file$index.pdf\"")
            val options = mapper.dynastyMultipartJson(body, "CaseAttachmentOptions")
            assertThat(options.at("/CaseSelector/Id").longValue()).isEqualTo(415)
            assertThat(options.at("/PhaseSelector/smartAttachOptions/pathSelector/0/Id").asText()).isEqualTo(phaseId.toString())
            assertThat(options.path("TOSTitle").asText()).isEqualTo(DynastyTosFixture.UEF_GRADUATION.documentTitles[index])
            assertThat(options.has("parentDocumentSelector")).isFalse()
            mapper.dynastyMultipartJson(body, "MetaData")
        }
        assertThat(documents).allSatisfy {
            assertThat(it["state"].asInt()).isEqualTo(3)
            assertThat(it["DeliveryType"].asText()).isEqualTo("M")
            assertThat(it["schemaId"].asText()).isEqualTo("document-schema")
            assertThat(it.fieldNames().asSequence().toList())
                .isSubsetOf(listOf("title", "schemaId", "AD", "DeliveryType", "state"))
        }
    }

    private fun specialtyType(prefix: String): ErikoisalaTyyppi =
        if (prefix == "EHL") ErikoisalaTyyppi.HAMMASLAAKETIEDE else ErikoisalaTyyppi.LAAKETIEDE
}
