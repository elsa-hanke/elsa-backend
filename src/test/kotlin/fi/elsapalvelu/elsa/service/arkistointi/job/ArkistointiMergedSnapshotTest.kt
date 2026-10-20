package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource

class ArkistointiMergedSnapshotTest {
    private val documents = mock<AsiakirjaRepository>()
    private val jobs = mock<ArkistointiJobRepository>()
    private val scheduler = mock<ArkistointiJobScheduler>()
    private val pdfA = spy(PdfA2bService(ClassPathResource("sRGB_CS_profile.icm")))
    private val snapshots = ArkistointiAsiakirjaSnapshotService(documents, pdfA)
    private val studyRight = Opintooikeus(id = 1L)
    private val original = Asiakirja(id = 10L, nimi = "original.pdf", tyyppi = "application/pdf", opintooikeus = studyRight,
        asiakirjaData = AsiakirjaData(data = PdfTestData.embeddedFontPdf(2)))
    private val ordinary = Asiakirja(id = 11L, nimi = "liitteet.pdf", tyyppi = "application/pdf", opintooikeus = studyRight,
        asiakirjaData = AsiakirjaData(data = PdfTestData.unembeddedFontPdf(1)))
    private val stored = mutableMapOf(10L to original, 11L to ordinary)

    @BeforeEach
    fun repository() {
        whenever(documents.findOneWithDataById(any())).thenAnswer { stored[it.getArgument<Long>(0)] }
        whenever(documents.save(any<Asiakirja>())).thenAnswer {
            it.getArgument<Asiakirja>(0).apply { id = 12L; stored[12L] = this }
        }
    }

    @Test
    fun `saves only the final snapshot and creator verifies integrity without another PDF-A parse`() {
        whenever(documents.findArchiveSnapshotFlagById(12L)).thenReturn(true)
        whenever(jobs.saveAndFlush(any<ArkistointiJob>())).thenAnswer { it.getArgument<ArkistointiJob>(0).apply { id = 42L } }
        whenever(scheduler.schedule(any(), any())).thenReturn(true)

        val reference = snapshots.createMergedReference(ordinary, listOf(original), RecordType.LIITE, 1)
        val bytes = reference.asiakirja.asiakirjaData!!.data!!
        verify(documents, times(1)).save(any<Asiakirja>())
        verify(pdfA, times(1)).isPdfA2b(argThat { contentEquals(bytes) })
        assertThat(reference.asiakirja.archiveSnapshot).isTrue()
        assertThat(reference.filename).isEqualTo("liitteet.pdf")
        assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(bytes))
        clearInvocations(pdfA)

        val job = ArkistointiJobCreatorImpl(jobs, scheduler, snapshots).create(CreateArkistointiJobRequest(
            YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", "merged-snapshot", listOf(reference)
        ))

        verify(pdfA, never()).isPdfA2b(any())
        assertThat(job.asiakirjat.single().asiakirja).isSameAs(reference.asiakirja)
    }

    @Test
    fun `rejects changed merged snapshot contents before scheduling`() {
        val reference = snapshots.createMergedReference(ordinary, listOf(original), RecordType.LIITE, 0)
        reference.asiakirja.asiakirjaData!!.data!![0] = 0

        assertThrows<ArkistointiAsiakirjaException> {
            ArkistointiJobCreatorImpl(jobs, scheduler, snapshots).create(CreateArkistointiJobRequest(
                YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", "tampered-merge", listOf(reference)
            ))
        }
        verify(scheduler, never()).schedule(any(), any())
    }

    @Test
    fun `uses persisted source bytes instead of caller supplied contents`() {
        val fabricated = original.copy(asiakirjaData = AsiakirjaData(data = byteArrayOf(1, 2, 3)))
        val reference = snapshots.createMergedReference(ordinary, listOf(fabricated), RecordType.LIITE, 0)
        assertThat(pdfA.isPdfA2b(reference.asiakirja.asiakirjaData!!.data)).isTrue()
    }

    @Test
    fun `rejects attachments of another study right without saving a snapshot`() {
        original.opintooikeus = Opintooikeus(id = 2L)
        assertThrows<ArkistointiAsiakirjaException> {
            snapshots.createMergedReference(ordinary, listOf(original), RecordType.LIITE, 0)
        }
        verify(documents, never()).save(any<Asiakirja>())
    }
}
