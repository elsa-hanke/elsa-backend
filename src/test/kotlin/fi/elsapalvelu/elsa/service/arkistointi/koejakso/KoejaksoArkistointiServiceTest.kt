package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.security.MDC_USER_ID_KEY
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobEnqueuer
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.slf4j.MDC

class KoejaksoArkistointiServiceTest {
    private val configuration = mock<ArkistointiConfigurationProvider>()
    private val synchronous = mock<SynchronousArkistointiService>()
    private val snapshots = mock<ArkistointiAsiakirjaSnapshotService>()
    private val creator = mock<ArkistointiJobCreator>()
    private val jobs = mock<ArkistointiJobRepository>()
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val service = KoejaksoArkistointiService(configuration, synchronous, ArkistointiJobEnqueuer(jobs, snapshots, creator, mapper))
    private val university = YliopistoEnum.HELSINGIN_YLIOPISTO
    private val studyRight = Opintooikeus(id = 12, yliopisto = Yliopisto(nimi = university), erikoisala = Erikoisala(id = 1))
    private val document = Asiakirja(id = 22, nimi = "approval.pdf", tyyppi = "application/pdf")

    @AfterEach
    fun clearMdc() = MDC.clear()

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `async job records the user from the request MDC and null when there is none`(type: CaseType) {
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(configuration.isAsyncEnabled(university, type)).thenReturn(true)
        val recordType = if (type == CaseType.SOPIMUS) RecordType.SOPIMUS else RecordType.ARVIOINTI
        val snapshot = Asiakirja(id = 33, nimi = "approval.pdf", tyyppi = "application/pdf", archiveSnapshot = true)
        whenever(snapshots.createReference(document, recordType, 0))
            .thenReturn(ArkistointiAsiakirjaReference(snapshot, recordType, 0, "approval.pdf", "application/pdf", "abc"))

        MDC.put(MDC_USER_ID_KEY, "approver-1")
        archive(type)
        MDC.remove(MDC_USER_ID_KEY)
        archive(type)

        val request = argumentCaptor<CreateArkistointiJobRequest>()
        verify(creator, times(2)).create(request.capture())
        assertThat(request.allValues.map { it.requestedByUserId }).containsExactly("approver-1", null)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `missing specialty prevents synchronous delivery instead of selecting non YEK`(type: CaseType) {
        studyRight.erikoisala = null
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(synchronous.muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )).thenReturn(ArkistointiPackage("/tmp/test.zip", null))

        assertThatThrownBy { archive(type) }.isInstanceOf(IllegalArgumentException::class.java)

        verify(synchronous, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        verifyNoInteractions(snapshots, creator, jobs)
    }

    @Test
    fun `assessment still requires specialty when archiving is disabled`() {
        studyRight.erikoisala = null

        assertThatThrownBy { archive(CaseType.KOEJAKSO) }.isInstanceOf(IllegalArgumentException::class.java)

        verifyNoInteractions(synchronous, snapshots, creator, jobs)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `disabled integration creates no snapshot or job`(type: CaseType) {
        archive(type)
        verifyNoInteractions(snapshots, creator, jobs)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `async preserves event metadata and creates one correct document reference`(type: CaseType) {
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(configuration.isAsyncEnabled(university, type)).thenReturn(true)
        val recordType = if (type == CaseType.SOPIMUS) RecordType.SOPIMUS else RecordType.ARVIOINTI
        val snapshot = Asiakirja(id = 33, nimi = "approval.pdf", tyyppi = "application/pdf", archiveSnapshot = true)
        val reference = ArkistointiAsiakirjaReference(snapshot, recordType, 0, "approval.pdf", "application/pdf", "abc")
        whenever(snapshots.createReference(document, recordType, 0)).thenReturn(reference)

        archive(type)

        val request = argumentCaptor<CreateArkistointiJobRequest>()
        verify(creator).create(request.capture())
        assertThat(request.firstValue.caseType).isEqualTo(type)
        assertThat(request.firstValue.asiakirjat).containsExactly(reference)
        val prefix = if (type == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        assertThat(request.firstValue.key).isEqualTo("$prefix:${university.name}:7")
        val payload = ArchivePayloadReader(mapper).read(request.firstValue.payload)
        val json = mapper.readTree(request.firstValue.payload)
        assertThat(json.has("licensingDate")).isFalse()
        assertThat(json.has("studyRight")).isFalse()
        assertThat(payload.caseId).isEqualTo(7)
        assertThat(payload.studyRightId).isEqualTo(12)
        assertThat(payload.documents).containsExactly(ArkistointiDocumentIdentity(22, 33, recordType, 0))
        assertThat(payload.approvalDate).isEqualTo(LocalDate.of(2026, 10, 24))
        val assessment = json["koejaksoHyvaksytty"]
        if (type == CaseType.KOEJAKSO) {
            assertThat(assessment.isBoolean).isTrue()
            assertThat(assessment.booleanValue()).isFalse()
        } else {
            assertThat(assessment.isNull).isTrue()
        }
        assertThat(payload.reviewer).isEqualTo(if (type == CaseType.SOPIMUS) "" else null)
        assertThat(payload.toString()).isEqualTo("ArchivePayload")
        verify(synchronous, never()).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )
        verify(synchronous, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `sync keeps existing package and delivery path`(type: CaseType) {
        // These values are required by async payload creation, but not by the legacy sender.
        studyRight.id = null
        document.id = null
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(synchronous.muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )).thenReturn(ArkistointiPackage("/tmp/test.zip", null))

        archive(type)

        val records = argumentCaptor<List<fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties>>()
        verify(synchronous).muodostaSahke(
            eq(studyRight), records.capture(), eq("7"), eq(if (type == CaseType.SOPIMUS) "" else null),
            eq(null), eq(null), eq(LocalDate.of(2026, 10, 24)), eq(university), eq(type)
        )
        assertThat(records.firstValue).hasSize(1)
        assertThat(records.firstValue.single().asiakirja).isSameAs(document)
        verify(synchronous).laheta(eq(university), eq("/tmp/test.zip"), eq(type), eq(false), eq("7"), anyOrNull())
        verifyNoInteractions(snapshots, creator, jobs)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `sync preserves YEK flag`(type: CaseType) {
        studyRight.erikoisala!!.id = YEK_ERIKOISALA_ID
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(synchronous.muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )).thenReturn(ArkistointiPackage("/tmp/test.zip", null))

        archive(type)

        verify(synchronous).laheta(eq(university), eq("/tmp/test.zip"), eq(type), eq(true), eq("7"), anyOrNull())
        verifyNoInteractions(snapshots, creator, jobs)
    }

    @ParameterizedTest
    @CsvSource(
        "SOPIMUS, ODOTTAA", "KOEJAKSO, ODOTTAA",
        "SOPIMUS, KASITTELYSSA", "KOEJAKSO, KASITTELYSSA",
        "SOPIMUS, LAHETETTY", "KOEJAKSO, LAHETETTY",
        "SOPIMUS, VAATII_TOIMENPITEITA", "KOEJAKSO, VAATII_TOIMENPITEITA"
    )
    fun `existing job in any status creates no snapshot job or synchronous delivery`(type: CaseType, status: ArkistointiJobStatus) {
        whenever(synchronous.onKaytossa(university, type)).thenReturn(true)
        whenever(configuration.isAsyncEnabled(university, type)).thenReturn(true)
        val prefix = if (type == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        val key = "$prefix:${university.name}:7"
        whenever(jobs.findByKey(key)).thenReturn(ArkistointiJob(status = status))

        archive(type)

        verify(jobs).findByKey(key)
        verifyNoInteractions(snapshots, creator)
        verify(synchronous, never()).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )
        verify(synchronous, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    private fun archive(type: CaseType) {
        val date = LocalDate.of(2026, 10, 24)
        if (type == CaseType.SOPIMUS) {
            service.archive(KoejaksonKoulutussopimus(id = 7, opintooikeus = studyRight, vastuuhenkilonKuittausaika = date), document)
        } else {
            service.archive(KoejaksonVastuuhenkilonArvio(
                id = 7, opintooikeus = studyRight, vastuuhenkilonKuittausaika = date, koejaksoHyvaksytty = false
            ), document)
        }
    }
}
