package fi.elsapalvelu.elsa.service.arkistointi.job.creation

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrors
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaReference
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ArkistointiJobCreatorImplTest {

    @Mock
    private lateinit var repository: ArkistointiJobRepository

    @Mock
    private lateinit var scheduler: ArkistointiJobScheduler

    @Mock
    private lateinit var snapshotService: ArkistointiAsiakirjaSnapshotService

    private lateinit var creator: ArkistointiJobCreatorImpl

    @BeforeEach
    fun setUp() {
        creator = ArkistointiJobCreatorImpl(repository, scheduler, snapshotService)
    }

    @Test
    fun `persists document references and schedules the job`() {
        val request = request()
        val jobId = UUID.randomUUID()
        val snapshot = request.asiakirjat.single().copy(filename = "snapshot.pdf", sha256 = "b".repeat(64))
        whenever(snapshotService.resolveReference(any())).thenReturn(snapshot)
        whenever(repository.saveAndFlush(any<ArkistointiJob>())).thenAnswer { invocation ->
            invocation.getArgument<ArkistointiJob>(0).apply { id = jobId }
        }
        whenever(scheduler.schedule(any(), any())).thenReturn(true)

        val saved = creator.create(request)

        assertThat(saved.id).isEqualTo(jobId)
        val jobCaptor = argumentCaptor<ArkistointiJob>()
        verify(repository).saveAndFlush(jobCaptor.capture())
        assertThat(jobCaptor.firstValue.asiakirjat).hasSize(1)
        assertThat(jobCaptor.firstValue.expectedDocumentCount).isEqualTo(1)
        assertThat(jobCaptor.firstValue.asiakirjat.single().sha256)
            .isEqualTo(snapshot.sha256)
        assertThat(jobCaptor.firstValue.asiakirjat.single().filename).isEqualTo(snapshot.filename)
        assertThat(saved.key).isEqualTo(request.key)
        assertThat(saved.id.toString()).isNotEqualTo(request.key)
        verify(scheduler).schedule(eq(jobId), any())
    }

    @Test
    fun `manifest count is fixed from the entire request before persist`() {
        val reference = request().asiakirjat.single()
        val request = request().copy(asiakirjat = listOf(
            reference.copy(jarjestysnumero = 5, asiakirjatyyppi = RecordType.LIITE), reference
        ))
        whenever(snapshotService.resolveReference(any())).thenAnswer { it.getArgument<ArkistointiAsiakirjaReference>(0) }
        whenever(repository.saveAndFlush(any<ArkistointiJob>())).thenAnswer {
            it.getArgument<ArkistointiJob>(0).apply {
                assertThat(expectedDocumentCount).isEqualTo(2)
                assertThat(asiakirjat.map { document -> document.jarjestysnumero }).containsExactly(0, 5)
                id = UUID.randomUUID()
            }
        }
        whenever(scheduler.schedule(any(), any())).thenReturn(true)

        assertThat(creator.create(request).expectedDocumentCount).isEqualTo(2)
    }

    @Test
    fun `existing business key returns the original manifest without snapshots or scheduling`() {
        val request = request()
        val existing = ArkistointiJob(id = UUID.randomUUID(), key = request.key,
            payload = "{\"original\":true}", expectedDocumentCount = 2)
        whenever(repository.findByKey(request.key)).thenReturn(existing)

        assertThat(creator.create(request)).isSameAs(existing)
        assertThat(existing.payload).isEqualTo("{\"original\":true}")
        assertThat(existing.expectedDocumentCount).isEqualTo(2)
        verify(repository, never()).saveAndFlush(any())
        verifyNoInteractions(snapshotService, scheduler)
    }

    @Test
    fun `checksum mismatch prevents job creation and scheduling`() {
        val request = request().copy(
            asiakirjat = listOf(request().asiakirjat.single().copy(sha256 = "a".repeat(64)))
        )
        whenever(snapshotService.resolveReference(any())).thenThrow(
            ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.checksumMismatch())
        )

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            creator.create(request)
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
        verify(repository, never()).saveAndFlush(any())
        verify(scheduler, never()).schedule(any(), any())
    }

    @Test
    fun `invalid document reference prevents job creation`() {
        val request = request()
        whenever(snapshotService.resolveReference(any())).thenThrow(
            ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.invalid())
        )

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            creator.create(request)
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_INVALID")
        verify(repository, never()).saveAndFlush(any())
    }

    private fun request(): CreateArkistointiJobRequest {
        val data = "document-content".toByteArray()
        val asiakirja = Asiakirja(
            id = 8L,
            nimi = "archive.pdf",
            tyyppi = "application/pdf",
            asiakirjaData = AsiakirjaData(data = data)
        )
        return CreateArkistointiJobRequest(
            university = YliopistoEnum.TURUN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN,
            payload = "{\"caseId\":123}",
            key = "valmistuminen-123",
            asiakirjat = listOf(
                ArkistointiAsiakirjaReference(
                    asiakirja = asiakirja,
                    asiakirjatyyppi = RecordType.YHTEENVETO,
                    jarjestysnumero = 0,
                    filename = "archive.pdf",
                    contentType = "application/pdf",
                    sha256 = ArkistointiChecksum.sha256(data)
                )
            )
        )
    }
}
