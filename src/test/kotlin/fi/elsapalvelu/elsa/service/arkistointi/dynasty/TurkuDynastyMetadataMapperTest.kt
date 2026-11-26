package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.DynastyMetadataMapper

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import com.fasterxml.jackson.databind.JsonNode

class TurkuDynastyMetadataMapperTest {
    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `valmistuminen maps program regulation dates and document specific fields`(kind: String) {
        val plan = plan(kind, false)
        val mapper = jacksonObjectMapper()
        val documents = plan.documents.map { mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(it) }
        assertThat(plan.tosClass).isEqualTo("05.06.02.04")
        assertThat(plan.task).isEqualTo("Hakeminen")
        val (summary, attachment, certificate) = documents
        // Type, retention and publicity come from the TOS title, so every document carries the same field set.
        documents.forEach { document ->
            assertThat(document.fieldNames().asSequence().toList())
                .containsExactlyInAnyOrder("title", "schemaId", "state", "DeliveryType", "languageId", "AD")
            assertThat(document.path("schemaId").asText()).isEqualTo(summary.path("schemaId").asText())
            assertThat(document.path("DeliveryType").asText()).isEqualTo("M")
            assertThat(document.path("languageId").asText()).isEqualTo("fi-FI")
        }
        assertThat(attachment.path("title").asText()).isEqualTo("Valmistumisen yhteenvedon liitteet Maija Meikäläinen 31.10.2026")
        assertThat(certificate.path("title").asText()).isEqualTo("Laillistustodistus Maija Meikäläinen")
        val case = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(plan.caseRequest).path("caseModel")
        assertThat(case.path("title").asText()).startsWith(kind)
        assertThat(case.has("retentionPeriod")).isFalse()
        assertThat(documents[0].path("AD").path("UTUkoulutusohjelma").asText()).isEqualTo(when(kind) {
            "YEK" -> "Yleislääketieteen erityiskoulutus"
            "EHL" -> "Erikoishammaslääkärikoulutus"
            else -> "Erikoislääkärikoulutus"
        })
        assertThat(documents[0].path("AD").path("UTUtutkintoasetus").asText()).isEqualTo("Confirmed regulation")
        assertThat(documents[0].path("AD").path("UTUalkamispaiva").asText()).isEqualTo("2020-01-01")
        assertThat(documents[0].path("AD").path("UTUvalmistumispaiva").asText()).isEqualTo("2026-10-31")
        assertThat(documents[0].path("AD").path("UTUlaillistamispaiva").asText()).isEqualTo("2019-01-01")
        assertThat(summary.path("AD").has("UTUhyvaksymispaiva")).isFalse()
        assertThat(summary.path("AD").path("ExtElsaTunniste").asText()).isNotBlank()
        val identity = """"UTUetunimi":"Maija","UTUsukunimi":"Meikäläinen","UTUtiedekunta":"Lääketieteellinen tiedekunta",
            "UTUkoulutusohjelma":"${documents[0].path("AD").path("UTUkoulutusohjelma").asText()}","UTUpaaaine":"Akuuttilääketiede""""
        assertThat(attachment.path("AD")).isEqualTo(mapper.readTree("{$identity}"))
        assertThat(certificate.path("AD")).isEqualTo(mapper.readTree("{$identity,\"UTUlaillistamispaiva\":\"2019-01-01\"}"))
    }

    @Test fun `koejakso maps secrecy dates and leaves retention to TOS`() {
        val plan = plan("EL", true)
        val doc = jacksonObjectMapper().valueToTree<JsonNode>(plan.documents.single())
        assertThat(plan.tosClass).isEqualTo("05.06.02.00")
        assertThat(plan.task).isEqualTo("Arviointi")
        assertThat(doc.path("title").asText()).isEqualTo("EL Koejakson vastuuhenkilön arvio Maija Meikäläinen 31.10.2026")
        assertThat(doc.fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("title", "schemaId", "state", "DeliveryType", "languageId", "AD", "SecrecyPeriod")
        assertThat(doc.path("languageId").asText()).isEqualTo("fi-FI")
        assertThat(doc.path("SecrecyPeriod").path("Start").asText()).isEqualTo("2026-11-19T00:00:00")
        assertThat(doc.path("SecrecyPeriod").path("End").asText()).isEqualTo("2126-11-19T00:00:00")
        assertThat(doc.path("AD").path("UTUhyvaksymispaiva").asText()).isEqualTo("2026-10-31")
        assertThat(doc.path("AD").has("UTUvalmistumispaiva")).isFalse()
        assertThat(doc.path("AD").has("ExtElsaTunniste")).isFalse()
    }

    @ParameterizedTest
    @CsvSource("false,turku-valmistuminen-el.json", "true,turku-koejakso-el.json")
    fun `plan matches the golden specification mapping`(koejakso: Boolean, goldenFile: String) {
        val mapper = jacksonObjectMapper()
        val actual = mapper.valueToTree<JsonNode>(plan("EL", koejakso))
        val golden = javaClass.getResourceAsStream("/fixtures/arkistointi/dynasty/$goldenFile")!!.use(mapper::readTree)
        assertThat(actual).isEqualTo(golden)
    }

    @Test
    fun `unconfigured degree regulation is rejected with regulation category`() {
        assertThatThrownBy { plan("EL", false) { it.put("degreeRegulation", "99/2000") } }
            .isInstanceOfSatisfying(DynastyFailure::class.java) {
                assertThat(it.category).isEqualTo(DynastyFailureCategory.REGULATION_UNCONFIGURED)
            }
    }

    @Test
    fun `snapshot missing specialty type is rejected`() {
        assertThatThrownBy { plan("EL", false) { it.remove("specialtyType") } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("unsupported specialty type")
    }

    @Test
    fun `snapshot missing required person data is rejected`() {
        assertThatThrownBy { plan("EL", true) { it.remove("firstName") } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("missing first name")
    }

    private fun plan(
        kind: String, koejakso: Boolean,
        changePayload: (com.fasterxml.jackson.databind.node.ObjectNode) -> Unit = {}
    ): DynastyMetadataMapper.Plan {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val properties = dynastyTestProperties("https://dynasty.example")
        val submission = dynastySubmission(
            mapper, caseType = if (koejakso) CaseType.KOEJAKSO else CaseType.VALMISTUMINEN,
            specialtyType = if (kind == "EHL") ErikoisalaTyyppi.HAMMASLAAKETIEDE else ErikoisalaTyyppi.LAAKETIEDE,
            yek = kind == "YEK"
        )
        val root = mapper.readTree(submission.payload) as com.fasterxml.jackson.databind.node.ObjectNode
        changePayload(root)
        val clock = Clock.fixed(Instant.parse("2026-11-18T22:30:00Z"), ZoneOffset.UTC)
        val settings = properties.getArkistointi().getTurku().dynasty
        return DynastyMetadataMapper(DynastyArchiveProfile.TURKU, settings, mapper, ArchivePayloadReader(mapper), clock)
            .plan(submission.copy(payload = root.toString()))
    }
}
