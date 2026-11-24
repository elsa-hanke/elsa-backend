package fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiTestData
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiTestScenario
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepKey
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepLedger
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveStudyRightPayload
import fi.elsapalvelu.elsa.service.arkistointi.support.arkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.koejakso.KoejaksoArkistointiPayload
import fi.elsapalvelu.elsa.service.arkistointi.valmistuminen.ValmistuminenArkistointiPayload
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.imageio.ImageIO

abstract class DynastyArkistointiExternalTestSupport : ExternalIntegrationTestSupport() {
    protected abstract val university: YliopistoEnum

    @Autowired
    private lateinit var adapter: DynastyArkistointiAdapter

    @Autowired
    private lateinit var ledger: DynastyStepLedger

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var clock: Clock

    @BeforeEach
    fun prepareLedger() {
        reset(ledger)
        whenever(ledger.begin(any(), any(), any())).thenReturn(DynastyStepLedger.Decision.Proceed)
        whenever(ledger.complete(any(), any(), any(), anyOrNull())).thenReturn(true)
    }

    protected fun archiveGraduation(programme: Programme, verifySavedCase: (String, List<String>) -> Unit = { _, _ -> }) {
        val (caseId, documentIds) = assertDeliverySucceeds(createGraduationSubmission(programme), "valmistuminen $programme")
        verifySavedCase(caseId, documentIds)
    }

    protected fun archiveKoejakso(
        programme: Programme,
        degreeRegulation: String = "56/2015",
        verifySavedCase: (String, List<String>, ClosedRange<LocalDate>) -> Unit
    ) {
        val submission = createKoejaksoSubmission(programme, degreeRegulation)
        val archiveClock = clock.withZone(DYNASTY_ARCHIVE_ZONE)
        val earliestDeliveryDate = LocalDate.now(archiveClock)
        val (caseId, documentIds) = assertDeliverySucceeds(submission, "koejakso $programme ($degreeRegulation)")
        verifySavedCase(caseId, documentIds, earliestDeliveryDate..LocalDate.now(archiveClock))
    }

    private fun assertDeliverySucceeds(submission: ArkistointiSubmission, description: String): Pair<String, List<String>> {
        log.info("Sending Dynasty archive test for {}: scenario={}, jobId={}", university, description, submission.jobId)
        val result = adapter.arkistoi(submission)

        assertThat(result).describedAs("Dynasty delivery must succeed for $description")
            .isInstanceOf(ArkistointiResult.Success::class.java)
        val caseId = requireNotNull((result as ArkistointiResult.Success).externalId) { "Unknown Dynasty case ID" }
        assertThat(caseId).isNotBlank()
        verify(ledger).complete(eq(submission.jobId), eq(DynastyStepKey.Case), eq(submission.execution.version), eq(caseId))
        if (university == YliopistoEnum.TURUN_YLIOPISTO) {
            verify(ledger, never()).begin(eq(submission.execution), eq(DynastyStepKey.Task), any())
        } else {
            verify(ledger).complete(eq(submission.jobId), eq(DynastyStepKey.Task), eq(submission.execution.version), any())
        }
        val documentIds = submission.asiakirjat.map { document ->
            val documentId = argumentCaptor<String>()
            verify(ledger).complete(
                eq(submission.jobId), eq(DynastyStepKey.Document(document.jarjestysnumero)), eq(submission.execution.version), documentId.capture()
            )
            documentId.singleValue
        }
        verify(ledger).complete(eq(submission.jobId), eq(DynastyStepKey.ArchiveCase), eq(submission.execution.version), isNull())
        log.info("Dynasty archive test succeeded for {}: jobId={}, caseId={}", university, submission.jobId, caseId)
        return caseId to documentIds
    }

    private fun createKoejaksoSubmission(programme: Programme, degreeRegulation: String): ArkistointiSubmission {
        val scenario = ArkistointiTestScenario(
            name = "Dynasty koejakso $programme",
            yliopisto = university,
            caseType = CaseType.KOEJAKSO,
            recordTypes = listOf(RecordType.ARVIOINTI),
            yek = programme == Programme.YEK
        )
        val input = ArkistointiTestData.createInput(scenario)
        val documents = loadDocuments(input.asiakirjat, pngCertificate = false)
        val payload = KoejaksoArkistointiPayload(
            caseId = requireNotNull(input.opintooikeus.id),
            studyRight = studyRightPayload(input.opintooikeus, scenario, programme, degreeRegulation),
            reviewer = "ELSA integraatiotesti", reviewDate = KOEJAKSO_REVIEW_DATE,
            approver = "ELSA integraatiotesti", approvalDate = KOEJAKSO_APPROVAL_DATE,
            koejaksoHyvaksytty = true,
            documents = documents.map {
                ArkistointiDocumentIdentity(it.asiakirjaId, it.asiakirjaId, it.asiakirjatyyppi, it.jarjestysnumero)
            }
        )
        return arkistointiSubmission(UUID.randomUUID(), scenario.yliopisto, scenario.caseType, mapper.writeValueAsString(payload), documents)
    }

    private fun studyRightPayload(
        opintooikeus: Opintooikeus, scenario: ArkistointiTestScenario, programme: Programme, degreeRegulation: String
    ): ArchiveStudyRightPayload {
        val testStudyRight = opintooikeus.copy(opintooikeudenMyontamispaiva = STUDY_RIGHT_START_DATE)
        return ArchiveStudyRightPayload.create(testStudyRight, scenario.yliopisto).copy(
            birthDate = BIRTH_DATE,
            yek = scenario.yek,
            specialtyType = programme.specialtyType,
            degreeRegulation = degreeRegulation
        )
    }

    // YEK certificates are uploaded as PNG images, so they are sent as image/png to real Dynasty.
    private fun loadDocuments(records: List<RecordProperties>, pngCertificate: Boolean) =
        records.mapIndexed { index, record ->
            val document = record.asiakirja
            val isPng = pngCertificate && record.type == RecordType.LAILLISTAMISTODISTUS
            val bytes = if (isPng) pngBytes() else requireNotNull(document.asiakirjaData?.data)
            val name = requireNotNull(document.nimi).let { if (isPng) "laillistamistodistus.png" else it }
            LoadedArkistointiAsiakirja(
                requireNotNull(document.id), record.type, index, name,
                if (isPng) "image/png" else "application/pdf", ArkistointiChecksum.sha256(bytes), bytes
            )
        }

    private fun pngBytes(): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()

    private fun createGraduationSubmission(programme: Programme): ArkistointiSubmission {
        val scenario = ArkistointiTestScenario(
            name = "Dynasty valmistuminen $programme",
            yliopisto = university,
            caseType = CaseType.VALMISTUMINEN,
            recordTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS),
            yek = programme == Programme.YEK
        )
        val input = ArkistointiTestData.createInput(scenario)
        val documents = loadDocuments(input.asiakirjat, pngCertificate = programme == Programme.YEK)
        val studyRight = studyRightPayload(input.opintooikeus, scenario, programme, degreeRegulation = "56/2015")
        val payload = ValmistuminenArkistointiPayload(
            caseId = requireNotNull(input.opintooikeus.id), studyRight = studyRight,
            reviewer = "ELSA integraatiotesti", reviewDate = GRADUATION_REVIEW_DATE,
            approver = "ELSA integraatiotesti", approvalDate = GRADUATION_APPROVAL_DATE,
            documents = documents.map {
                ArkistointiDocumentIdentity(it.asiakirjaId, it.asiakirjaId, it.asiakirjatyyppi, it.jarjestysnumero)
            },
            licensingDate = LICENSING_DATE
        )
        return arkistointiSubmission(UUID.randomUUID(), scenario.yliopisto, scenario.caseType, mapper.writeValueAsString(payload), documents)
    }

    enum class Programme(val specialtyType: ErikoisalaTyyppi) {
        EL(ErikoisalaTyyppi.LAAKETIEDE),
        EHL(ErikoisalaTyyppi.HAMMASLAAKETIEDE),
        YEK(ErikoisalaTyyppi.LAAKETIEDE)
    }

    // Distinct dates let the archive verify that each Dynasty date field comes from the right Elsa field.
    private companion object {
        val BIRTH_DATE: LocalDate = LocalDate.of(1985, 3, 12)
        val LICENSING_DATE: LocalDate = LocalDate.of(2019, 6, 14)
        val STUDY_RIGHT_START_DATE: LocalDate = LocalDate.of(2020, 8, 1)
        val KOEJAKSO_REVIEW_DATE: LocalDate = LocalDate.of(2026, 8, 18)
        val KOEJAKSO_APPROVAL_DATE: LocalDate = LocalDate.of(2026, 9, 21)
        val GRADUATION_REVIEW_DATE: LocalDate = LocalDate.of(2026, 9, 7)
        val GRADUATION_APPROVAL_DATE: LocalDate = LocalDate.of(2026, 10, 15)
    }
}
