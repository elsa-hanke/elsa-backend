package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class DynastyRequestsTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `case DTO keeps vendor field casing and omits absent additional data`() {
        val request = DynastyCaseRequest(DynastyCaseModel("schema", "Test title", DynastyAdditionalData(fieldPrefix = "UTU", firstName = "Test")))
        assertThat(mapper.readTree(mapper.writeValueAsString(request))).isEqualTo(mapper.readTree("""{
            "caseModel": {"schemaid":"schema", "title":"Test title", "publicityClassId":"J",
                "publicityClassName":"Julkinen", "manner":"M", "AD":{"UTUetunimi":"Test"}}
        }"""))
    }

    @Test
    fun `TOS phase selectors preserve vendor casing and JSON number types`() {
        val taskId = DynastyTaskId(UUID.fromString("8ae4f236-0cb5-4028-b4d2-c25da9643285"))
        val tos = DynastyTosPhaseOptions(DynastyCaseId(415), taskId, "TOS title")
        assertThat(mapper.readTree(mapper.writeValueAsString(tos))).isEqualTo(mapper.readTree("""{
            "CaseSelector":{"Id":415},
            "PhaseSelector":{"smartAttachOptions":{"pathSelector":[{"Id":"8ae4f236-0cb5-4028-b4d2-c25da9643285"}]}},
            "TOSTitle":"TOS title"
        }"""))
    }

    @Test
    fun `document DTO serializes the secrecy period`() {
        val metadata = DynastyTosDocumentMetadata("Test title", "schema", DynastyAdditionalData(fieldPrefix = "UEF"),
            secrecyPeriod = DynastySecrecyPeriod(java.time.LocalDate.of(2026, 11, 19), java.time.LocalDate.of(2126, 11, 19)))
        assertThat(mapper.readTree(mapper.writeValueAsString(metadata))).isEqualTo(mapper.readTree("""{
            "title":"Test title", "schemaId":"schema", "AD":{}, "DeliveryType":"M", "state":3,
            "SecrecyPeriod":{"Start":"2026-11-19T00:00:00","End":"2126-11-19T00:00:00"}
        }"""))
    }
}
