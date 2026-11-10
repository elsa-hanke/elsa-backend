package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveStudyRightPayload
import fi.elsapalvelu.elsa.service.arkistointi.koejakso.KoejaksoArkistointiPayload
import fi.elsapalvelu.elsa.service.arkistointi.support.arkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.valmistuminen.ValmistuminenArkistointiPayload
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.LocalDate
import java.util.UUID

const val DYNASTY_TEST_TOKEN_RESPONSE = """{"access_token":"test","expires_in":900}"""

fun dynastyTestProperties(baseUrl: String): ApplicationProperties = ApplicationProperties().apply {
    getArkistointi().getTurku().apply {
        dynasty.apply {
            this.baseUrl = baseUrl.trimEnd('/')
            tokenUrl = "${this.baseUrl}/token"
            clientId = "client"
            clientSecret = "secret"
            organization = "0"
            caseSchemaId = "case-schema"
            documentSchemaId = "document-schema"
            allowLoopbackHttpForTests = true
            tutkintoasetukset = mapOf("56/2015" to "Confirmed regulation")
            koejaksoPublicityClassId = "test-secret"
            koejaksoSecrecyFields = mapOf("reason" to "test-law", "duration" to "100", "start" to "approval")
        }
        metadata = ApplicationProperties.Arkistointi.Metadata().apply {
            cases = mapOf(
                "valmistuminen" to ApplicationProperties.Arkistointi.Case().apply {
                    documents = listOf("yhteenveto", "liite", "laillistamistodistus").associateWith { type ->
                        ApplicationProperties.Arkistointi.DocumentMetadata().apply { retentionPeriod = if (type == "yhteenveto") "20" else "10" }
                    }
                },
                "koejakso" to ApplicationProperties.Arkistointi.Case().apply {
                    documents = mapOf("arviointi" to ApplicationProperties.Arkistointi.DocumentMetadata().apply { retentionPeriod = "-1" })
                }
            )
        }
    }
}

/** Valid serialized approval; malformed-field mutations remain explicit in the calling test. */
fun dynastySubmission(
    mapper: ObjectMapper,
    caseType: CaseType = CaseType.VALMISTUMINEN,
    specialtyType: ErikoisalaTyyppi = ErikoisalaTyyppi.LAAKETIEDE,
    yek: Boolean = false,
    jobId: UUID = UUID.randomUUID(),
    version: Long = 1
): ArkistointiSubmission {
    require(caseType in setOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO))
    val types = if (caseType == CaseType.VALMISTUMINEN) listOf(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
        else listOf(RecordType.ARVIOINTI)
    val documents = types.mapIndexed { index, type ->
        LoadedArkistointiAsiakirja(index.toLong() + 1, type, index, "file$index.pdf", "application/pdf", "checksum", byteArrayOf(1, 2, 3))
    }
    val identities = documents.map { document ->
        ArkistointiDocumentIdentity(
            document.asiakirjaId, document.asiakirjaId, document.asiakirjatyyppi, document.jarjestysnumero
        )
    }
    val studyRight = ArchiveStudyRightPayload(
        opintooikeusId = 2, university = YliopistoEnum.TURUN_YLIOPISTO, yek = yek,
        traineeName = "Maija Meikäläinen", firstName = "Maija", lastName = "Meikäläinen",
        birthDate = LocalDate.of(1980, 1, 1), specialtyId = 1, specialtyName = "Akuuttilääketiede", studentNumber = null,
        specialtyType = specialtyType, studyRightStartDate = LocalDate.of(2020, 1, 1), degreeRegulation = "56/2015"
    )
    val approvalDate = LocalDate.of(2026, 10, 31)
    val payload = if (caseType == CaseType.VALMISTUMINEN) {
        ValmistuminenArkistointiPayload(1, studyRight, null, null, null, approvalDate, identities, LocalDate.of(2019, 1, 1))
    } else {
        KoejaksoArkistointiPayload(1, studyRight, null, null, null, approvalDate, null, identities)
    }
    return arkistointiSubmission(jobId, studyRight.university, caseType, mapper.writeValueAsString(payload), documents, version)
}

/** Standard successful HTTP sequence; tests with rejection or ambiguity enqueue their own responses. */
fun MockWebServer.enqueueDynastySuccess(documentIds: List<Long>, caseId: Long = 415, taskId: UUID = UUID.randomUUID()) {
    listOf(DYNASTY_TEST_TOKEN_RESPONSE, caseId.toString(), "\"$taskId\"").forEach { enqueue(MockResponse().setBody(it)) }
    documentIds.forEach { enqueue(MockResponse().setBody(it.toString())) }
    enqueue(MockResponse().setBody(""))
}
