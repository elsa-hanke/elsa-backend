package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class TurkuDynastyMetadataMapperTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `graduation maps program regulation dates and document specific fields`(kind: String) {
        val plan = plan(kind, false)
        val mapper = jacksonObjectMapper()
        val documents = plan.documents.map(mapper::readTree)
        assertThat(plan.tosClass).isEqualTo("05.06.02.04")
        assertThat(mapper.readTree(plan.caseJson).path("caseModel").path("title").asText()).startsWith(kind)
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
        val doc = jacksonObjectMapper().readTree(plan.documents.single())
        assertThat(plan.tosClass).isEqualTo("05.06.02.00")
        assertThat(doc.path("publicityClassName").asText()).isEqualTo("Osittain salassa pidettävä")
        assertThat(doc.path("publicityClassId").asText()).isEqualTo("test-secret")
        assertThat(doc.path("duration").asText()).isEqualTo("100")
        assertThat(doc.path("retentionPeriodLength").asInt()).isEqualTo(-1)
        assertThat(doc.path("AD").path("UTUhyvaksymispaiva").asText()).isEqualTo("2026-10-31")
        assertThat(doc.path("AD").has("UTUvalmistumispaiva")).isFalse()
        assertThat(doc.path("AD").has("UTUlaillistamispaiva")).isFalse()
    }
    private fun plan(kind: String, koejakso: Boolean): TurkuDynastyMetadataMapper.Plan = MockWebServer().use { server ->
        server.start()
            val mapper = jacksonObjectMapper()
            val properties = ApplicationProperties()
            properties.getArkistointi().getTurku().apply {
                dynasty.apply {
                    baseUrl = server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/')
                    tokenUrl = "$baseUrl/token"
                    clientId = "client"; clientSecret = "secret"; organization = "0"
                    caseSchemaId = "case-schema"; documentSchemaId = "document-schema"
                    allowLoopbackHttpForTests = true
                    tutkintoasetukset = mapOf("56/2015" to "Confirmed regulation")
                }
                metadata = ApplicationProperties.Arkistointi.Metadata().apply {
                    cases = mapOf("valmistuminen" to ApplicationProperties.Arkistointi.Case().apply {
                        documents = listOf("yhteenveto","liite","laillistamistodistus").associateWith {
                            ApplicationProperties.Arkistointi.DocumentMetadata().apply { retentionPeriod = if (it == "yhteenveto") "20" else "10" }
                        }
                    })
                }
            }
            properties.getArkistointi().getTurku().apply {
                dynasty.koejaksoPublicityClassId = "test-secret"
                dynasty.koejaksoSecrecyFields = mapOf("reason" to "test-law", "duration" to "100", "start" to "approval")
                val cases = metadata!!.cases!!.toMutableMap()
                cases["koejakso"] = ApplicationProperties.Arkistointi.Case().apply {
                    documents = mapOf("arviointi" to ApplicationProperties.Arkistointi.DocumentMetadata().apply { retentionPeriod = "-1" })
                }
                metadata!!.cases = cases
            }
            val types = if (koejakso) listOf(RecordType.ARVIOINTI) else listOf(RecordType.YHTEENVETO,RecordType.LIITE,RecordType.LAILLISTAMISTODISTUS)
            val documents = types.mapIndexed { index, type ->
                LoadedArkistointiAsiakirja(index.toLong()+1,type,index,"file$index.pdf","application/pdf","checksum",byteArrayOf(1,2,3))
            }
            val payload = mapper.writeValueAsString(mapOf(
                "caseId" to 1,"valmistumispyyntoId" to 1,"opintooikeusId" to 2,"university" to "TURUN_YLIOPISTO","yek" to (kind == "YEK"),
                "firstName" to "Maija","lastName" to "Meikäläinen","specialtyName" to "Akuuttilääketiede",
                "specialtyType" to if (kind == "EHL") "HAMMASLAAKETIEDE" else "LAAKETIEDE","approvalDate" to "2026-10-31","birthDate" to "1980-01-01",
                "degreeRegulation" to "56/2015","studyRightStartDate" to "2020-01-01","licensingDate" to "2019-01-01",
                "documents" to documents.map {
                    mapOf("sourceDocumentId" to it.asiakirjaId,"snapshotDocumentId" to it.asiakirjaId,
                        "recordType" to it.asiakirjatyyppi.name,"jarjestysnumero" to it.jarjestysnumero)
                }
            ))
            val execution = ArkistointiJobExecution("archive",UUID.randomUUID().toString(),1)
            val submission = ArkistointiSubmission(
                execution.jobId, YliopistoEnum.TURUN_YLIOPISTO,
                if (koejakso) CaseType.KOEJAKSO else CaseType.VALMISTUMINEN,
                payload, documents, execution
            )
        TurkuDynastyMetadataMapper(properties,mapper,SahkePayloadReader(mapper)).plan(submission)
    }
}
