package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TurkuDynastyMetadataMapperTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `graduation maps program regulation dates and document specific fields`(kind: String) {
        val plan = plan(kind, false)
        val mapper = jacksonObjectMapper()
        val documents = plan.documents.map { mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(it) }
        assertThat(plan.tosClass).isEqualTo("05.06.02.04")
        assertThat(mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(plan.caseRequest).path("caseModel").path("title").asText()).startsWith(kind)
        assertThat(documents[0].path("AD").path("UTUkoulutusohjelma").asText()).isEqualTo(when(kind) {
            "YEK" -> "Yleislääketieteen erityiskoulutus"
            "EHL" -> "Erikoishammaslääkärikoulutus"
            else -> "Erikoislääkärikoulutus"
        })
        assertThat(documents[0].path("AD").path("UTUtutkintoasetus").asText()).isEqualTo("Confirmed regulation")
        assertThat(documents[0].path("AD").path("UTUalkamispaiva").asText()).isEqualTo("2020-01-01")
        assertThat(documents[0].path("AD").path("UTUvalmistumispaiva").asText()).isEqualTo("2026-10-31")
        assertThat(documents[0].path("AD").path("UTUlaillistamispaiva").asText()).isEqualTo("2019-01-01")
        assertThat(documents[1].path("AD").has("UTUlaillistamispaiva")).isFalse()
        assertThat(documents[1].path("AD").has("UTUtutkintoasetus")).isFalse()
        assertThat(documents[2].path("AD").path("UTUlaillistamispaiva").asText()).isEqualTo("2019-01-01")
        assertThat(documents[2].path("AD").has("UTUvalmistumispaiva")).isFalse()
        assertThat(documents.map { it.path("retentionPeriodLength").asInt() }).containsExactly(20,10,10)
    }
    @Test fun `koejakso maps secrecy approval and permanent retention`() {
        val plan = plan("EL", true)
        val doc = jacksonObjectMapper().valueToTree<com.fasterxml.jackson.databind.JsonNode>(plan.documents.single())
        assertThat(plan.tosClass).isEqualTo("05.06.02.00")
        assertThat(doc.path("publicityClassName").asText()).isEqualTo("Osittain salassa pidettävä")
        assertThat(doc.path("publicityClassId").asText()).isEqualTo("test-secret")
        assertThat(doc.path("duration").asText()).isEqualTo("100")
        assertThat(doc.path("retentionPeriodLength").asInt()).isEqualTo(-1)
        assertThat(doc.path("AD").path("UTUhyvaksymispaiva").asText()).isEqualTo("2026-10-31")
        assertThat(doc.path("AD").has("UTUvalmistumispaiva")).isFalse()
        assertThat(doc.path("AD").has("UTUlaillistamispaiva")).isFalse()
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
    ): TurkuDynastyMetadataMapper.Plan {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val properties = dynastyTestProperties("https://dynasty.example")
        val submission = dynastySubmission(
            mapper, caseType = if (koejakso) CaseType.KOEJAKSO else CaseType.VALMISTUMINEN,
            specialtyType = if (kind == "EHL") ErikoisalaTyyppi.HAMMASLAAKETIEDE else ErikoisalaTyyppi.LAAKETIEDE,
            yek = kind == "YEK"
        )
        val root = mapper.readTree(submission.payload) as com.fasterxml.jackson.databind.node.ObjectNode
        changePayload(root)
        return TurkuDynastyMetadataMapper(properties, mapper, ArchivePayloadReader(mapper)).plan(submission.copy(payload = root.toString()))
    }
}
