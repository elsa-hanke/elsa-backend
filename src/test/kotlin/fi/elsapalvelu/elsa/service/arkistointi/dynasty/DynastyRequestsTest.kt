package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class DynastyRequestsTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `case DTO keeps vendor field casing and omits absent additional data`() {
        val request = DynastyCaseRequest(DynastyCaseModel("schema", "Test title", DynastyAdditionalData(firstName = "Test")))
        assertThat(mapper.readTree(mapper.writeValueAsString(request))).isEqualTo(mapper.readTree("""{
            "caseModel": {"schemaid":"schema", "title":"Test title", "publicityClassId":"J",
                "publicityClassName":"Julkinen", "manner":"M", "AD":{"UTUetunimi":"Test"}}
        }"""))
    }

    @Test
    fun `main document and attachment selectors preserve vendor casing and JSON number types`() {
        val taskId = DynastyTaskId(UUID.fromString("8ae4f236-0cb5-4028-b4d2-c25da9643285"))
        val main = DynastyMainDocumentOptions(DynastyCaseId(415), taskId)
        val attachment = DynastyAttachmentOptions(DynastyCaseId(415), DynastyDocumentId(1078))
        assertThat(mapper.readTree(mapper.writeValueAsString(main))).isEqualTo(mapper.readTree("""{
            "CaseSelector":{"Id":"415"}, "PhaseSelector":{"Id":"8ae4f236-0cb5-4028-b4d2-c25da9643285"}
        }"""))
        assertThat(mapper.readTree(mapper.writeValueAsString(attachment))).isEqualTo(mapper.readTree("""{
            "caseSelector":{"id":415}, "parentDocumentSelector":{"id":1078}
        }"""))
    }

    @Test
    fun `document DTO flattens only configured secrecy fields`() {
        val metadata = DynastyDocumentMetadata("Test title", "schema", "Assessment", -1, DynastyAdditionalData(),
            "secret", "Osittain salassa pidettävä", "Asiakirjan päivämäärä", secrecyFields = mapOf("reason" to "law", "duration" to "100", "start" to "approval"))
        assertThat(mapper.readTree(mapper.writeValueAsString(metadata))).isEqualTo(mapper.readTree("""{
            "title":"Test title", "schemaId":"schema", "documentType":"Assessment", "retentionPeriodLength":-1, "AD":{},
            "publicityClassId":"secret", "publicityClassName":"Osittain salassa pidettävä", "retentionPeriodRule":"Asiakirjan päivämäärä",
            "personalData":"Sisältää henkilötietoja", "state":3, "reason":"law", "duration":"100", "start":"approval"
        }"""))
    }
}
