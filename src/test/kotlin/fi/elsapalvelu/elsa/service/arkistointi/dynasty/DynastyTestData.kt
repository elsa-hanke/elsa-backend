package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.fasterxml.jackson.databind.JsonNode
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

fun dynastyTestProperties(
    baseUrl: String, university: YliopistoEnum = YliopistoEnum.TURUN_YLIOPISTO
): ApplicationProperties = ApplicationProperties().apply {
    val archive = DynastyArchiveProfile.from(university).archiveProperties(getArkistointi())
    archive.dynasty.apply {
        this.baseUrl = baseUrl.trimEnd('/')
        tokenUrl = "${this.baseUrl}/token"
        clientId = "client"
        clientSecret = "secret"
        organization = "0"
        caseSchemaId = "case-schema"
        documentSchemaId = "document-schema"
        allowLoopbackHttpForTests = true
        tutkintoasetukset = mapOf("56/2015" to "Confirmed regulation")
    }
}

/** Valid serialized approval; malformed-field mutations remain explicit in the calling test. */
fun dynastySubmission(
    mapper: ObjectMapper,
    caseType: CaseType = CaseType.VALMISTUMINEN,
    specialtyType: ErikoisalaTyyppi = ErikoisalaTyyppi.LAAKETIEDE,
    yek: Boolean = false,
    jobId: UUID = UUID.randomUUID(),
    version: Long = 1,
    university: YliopistoEnum = YliopistoEnum.TURUN_YLIOPISTO
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
        opintooikeusId = 2, university = university, yek = yek,
        traineeName = "Maija Meikäläinen", firstName = "Maija", lastName = "Meikäläinen",
        birthDate = LocalDate.of(1980, 1, 1), specialtyId = 1, specialtyName = "Akuuttilääketiede", studentNumber = null,
        specialtyType = specialtyType, studyRightStartDate = LocalDate.of(2020, 1, 1), degreeRegulation = "56/2015"
    )
    val reviewDate = LocalDate.of(2026, 10, 28)
    val approvalDate = LocalDate.of(2026, 10, 31)
    val payload = if (caseType == CaseType.VALMISTUMINEN) {
        ValmistuminenArkistointiPayload(1, studyRight, null, reviewDate, null, approvalDate, identities, LocalDate.of(2019, 1, 1))
    } else {
        KoejaksoArkistointiPayload(1, studyRight, null, reviewDate, null, approvalDate, null, identities)
    }
    return arkistointiSubmission(jobId, studyRight.university, caseType, mapper.writeValueAsString(payload), documents, version)
}

internal enum class DynastyTosFixture(
    val tosClass: String, val phase: String, val documentTitle: String, val additionalTitles: List<String> = emptyList()
) {
    GRADUATION("05.06.02.04", "Hakeminen", "Valmistumisen yhteenveto,  EL, EHL tai YEK",
        listOf("Valmistumisen yhteenvedon liitteet,  EL, EHL tai YEK")),
    ASSESSMENT("05.06.02.00", "Arviointi", "Koejakson vastuuhenkilön arvio"),
    UEF_GRADUATION("03.11", "ELSA valmistumispyyntö ja sen liitteet",
        "Valmistumisen yhteenveto, EL, EHL tai YEK (valmistumispyyntö)",
        listOf("Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK", "Laillistustodistus (valmistumisen yhteenvedon liite)")),
    UEF_ASSESSMENT("03.11", "ELSA Koejaksojen yhteenvedot", "Koejakson vastuuhenkilön arvio");

    val path get() = "/api/v1/cases/phases/$tosClass"
    val documentTitles get() = listOf(documentTitle) + additionalTitles
    private val documentTypes get() = documentTitles.joinToString(",") { """{"title":"$it","childs":[]}""" }

    fun response(requestPath: String, phaseId: UUID = PHASE_ID): MockResponse? = when (requestPath) {
        path -> MockResponse().setBody(
            """{"fullCasegroupCode":"$tosClass","thsPhases":[{"title":"$phase","taskRowId":"$phaseId"}]}"""
        )
        "$path/$phaseId/documenttypes" -> MockResponse().setBody(
            """{"fullCasegroupCode":"$tosClass","taskRowId":"$phaseId","thsDocuments":[$documentTypes]}"""
        )
        else -> null
    }

    fun MockWebServer.enqueueLookup(phaseId: UUID = PHASE_ID) {
        enqueue(requireNotNull(response(path, phaseId)))
        enqueue(requireNotNull(response("$path/$phaseId/documenttypes", phaseId)))
    }

    fun enqueueSuccess(server: MockWebServer, documentIds: List<Long>, phaseId: UUID = PHASE_ID) = with(server) {
        enqueue(MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE))
        enqueueLookup(phaseId)
        (listOf("415") + documentIds.map(Long::toString) + "").forEach { enqueue(MockResponse().setBody(it)) }
    }

    companion object {
        const val ATTACHMENT_TITLE = "Valmistumisen yhteenvedon liitteet,  EL, EHL tai YEK"
        val PHASE_ID: UUID = UUID.fromString("68630ae0-34d8-43a5-93de-0a7219eecc60")
    }
}

fun ObjectMapper.dynastyMultipartJson(body: String, part: String): JsonNode = readTree(
    Regex("name=\"$part\"\\r\\n(?:[A-Za-z-]+: [^\\r\\n]*\\r\\n)*\\r\\n(.*?)\\r\\n--", RegexOption.DOT_MATCHES_ALL)
        .find(body)!!.groupValues[1]
)
