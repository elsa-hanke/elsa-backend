package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ArkistointiJobCreatorImplTest {

    @Mock
    private lateinit var repository: ArkistointiJobRepository

    @Mock
    private lateinit var scheduler: ArkistointiJobScheduler

    @Mock
    private lateinit var pdfA2bService: PdfA2bService

    private lateinit var creator: ArkistointiJobCreatorImpl

    @BeforeEach
    fun setUp() {
        creator = ArkistointiJobCreatorImpl(repository, scheduler, pdfA2bService)
    }

    @Test
    fun `persists document references and schedules the job`() {
        val request = request()
        whenever(pdfA2bService.isPdfA2b(any())).thenReturn(true)
        whenever(repository.saveAndFlush(any<ArkistointiJob>())).thenAnswer { invocation ->
            invocation.getArgument<ArkistointiJob>(0).apply { id = 42L }
        }
        whenever(scheduler.schedule(any(), any())).thenReturn(true)

        val saved = creator.create(request)

        assertThat(saved.id).isEqualTo(42L)
        val jobCaptor = argumentCaptor<ArkistointiJob>()
        verify(repository).saveAndFlush(jobCaptor.capture())
        assertThat(jobCaptor.firstValue.asiakirjat).hasSize(1)
        assertThat(jobCaptor.firstValue.asiakirjat.single().sha256)
            .isEqualTo(request.asiakirjat.single().sha256)
        verify(scheduler).schedule(any(), any())
    }

    @Test
    fun `checksum mismatch prevents job creation and scheduling`() {
        val request = request().copy(
            asiakirjat = listOf(request().asiakirjat.single().copy(sha256 = "a".repeat(64)))
        )
        whenever(pdfA2bService.isPdfA2b(any())).thenReturn(true)

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            creator.create(request)
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
        verify(repository, never()).saveAndFlush(any())
        verify(scheduler, never()).schedule(any(), any())
    }

    @Test
    fun `non PDF-A document prevents job creation`() {
        val request = request()
        whenever(pdfA2bService.isPdfA2b(any())).thenReturn(false)

        val exception = assertThrows<ArkistointiAsiakirjaException> {
            creator.create(request)
        }

        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_NOT_PDFA_2B")
        verify(repository, never()).saveAndFlush(any())
    }

    private fun request(): CreateArkistointiJobRequest {
        val data = "pdf-a-content".toByteArray()
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
