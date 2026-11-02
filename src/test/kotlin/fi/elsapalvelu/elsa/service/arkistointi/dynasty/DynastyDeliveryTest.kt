package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class DynastyDeliveryTest {
    @Test
    @Suppress("LongMethod") // One complete multipart delivery and resume scenario.
    fun `retry resumes rejected attachment and never recreates case task or main document`() {
        MockWebServer().use { server ->
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
            val types = listOf(RecordType.YHTEENVETO,RecordType.LIITE,RecordType.LAILLISTAMISTODISTUS)
            val documents = types.mapIndexed { index, type ->
                LoadedArkistointiAsiakirja(index.toLong()+1,type,index,"file$index.pdf","application/pdf","checksum",byteArrayOf(1,2,3))
            }
            val payload = mapper.writeValueAsString(mapOf(
                "valmistumispyyntoId" to 1,"opintooikeusId" to 2,"university" to "TURUN_YLIOPISTO","yek" to false,
                "firstName" to "Maija","lastName" to "Meikäläinen","specialtyName" to "Akuuttilääketiede",
                "specialtyType" to "LAAKETIEDE","approvalDate" to "2026-10-31","birthDate" to "1980-01-01",
                "degreeRegulation" to "56/2015","studyRightStartDate" to "2020-01-01","licensingDate" to "2019-01-01",
                "documents" to documents.map {
                    mapOf("sourceDocumentId" to it.asiakirjaId,"snapshotDocumentId" to it.asiakirjaId,
                        "recordType" to it.asiakirjatyyppi.name,"jarjestysnumero" to it.jarjestysnumero)
                }
            ))
            val execution = ArkistointiJobExecution("archive",UUID.randomUUID().toString(),1)
            val submission = ArkistointiSubmission(execution.jobId,YliopistoEnum.TURUN_YLIOPISTO,CaseType.VALMISTUMINEN,payload,documents,execution)
            val ledger = mock<DynastyStepLedger>()
            val steps = mutableMapOf<String,DynastyStepLedger.Decision>()
            whenever(ledger.begin(eq(execution),any())).thenAnswer {
                val key = it.getArgument<String>(1)
                steps[key] ?: DynastyStepLedger.Decision.Proceed.also { steps[key] = DynastyStepLedger.Decision.Unconfirmed }
            }
            whenever(ledger.complete(any(),any(),any(),anyOrNull())).thenAnswer {
                steps[it.getArgument(1)] = DynastyStepLedger.Decision.Done(it.getArgument(3)); true
            }
            doAnswer { steps.remove(it.getArgument<String>(1)); null }.whenever(ledger).abandon(any(),any(),any())
            val adapter = TurkuDynastyArkistointiAdapter(DynastyClient(properties,mapper),TurkuDynastyMetadataMapper(properties,mapper,ArchivePayloadReader(mapper)),ledger,mapper)
            listOf("""{"access_token":"token","expires_in":900}""","415","\"${UUID.randomUUID()}\"","1078").forEach { server.enqueue(MockResponse().setBody(it)) }
            server.enqueue(MockResponse().setResponseCode(403))
            assertThat(adapter.arkistoi(submission)).isInstanceOf(ArkistointiResult.PermanentFailure::class.java)
            listOf("1079","1080","").forEach { server.enqueue(MockResponse().setBody(it)) }
            assertThat(adapter.arkistoi(submission)).isEqualTo(ArkistointiResult.Success("415"))
            val requests = (1..8).map { server.takeRequest() }
            assertThat(requests.count { it.path!!.startsWith("/api/v1/cases?") }).isEqualTo(1)
            val main = requests[3].body.readUtf8()
            assertThat(main).contains("MetaData","CaseAttachmentOptions","File","PhaseSelector","UTUvalmistumispaiva","Confirmed regulation")
            assertThat(requests[5].body.readUtf8()).contains("parentDocumentSelector","1078")
            assertThat(requests.last().path).isEqualTo("/api/v1/cases/415/state/Archived")
        }
    }
}
