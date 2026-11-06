package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfDocument
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.arkistointi.job.document.toDescriptor
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import java.io.ByteArrayInputStream
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.MediaType

/** Large attachments are frozen into the snapshot without losing pages, text or CMYK image data. */
class ValmistuminenLongAttachmentIT : ValmistuminenArchiveItBase() {

    @ParameterizedTest
    @ValueSource(ints = [30, 100, 201])
    fun `async approval freezes long PDF attachments without losing pages or text`(pages: Int) {
        val original = PdfTestData.unembeddedFontPdf(pages)
        val fixture = fixture("EL", attachment = original)

        approve(fixture)

        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val job = jobs.findByKey(key(fixture))!!
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
            assertQueuedDocumentsMatchSources(job, request)
            assertSnapshots(job)
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val archived = loader.load(job.asiakirjat.single { document -> document.asiakirjatyyppi == RecordType.LIITE }.toDescriptor()).sisalto
            assertAttachmentPdf(archived, pages)
            assertThat(em.find(Asiakirja::class.java, fixture.attachmentId).asiakirjaData!!.data).containsExactly(*original)
        }
        verify(transport, never()).laheta(any(), any())
        verify(legacyArchive, never()).muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `async approval freezes the ordinary merge of a long PDF and CMYK image`(embeddedFont: Boolean) {
        val pages = 100
        val original = if (embeddedFont) PdfTestData.embeddedFontPdf(pages) else PdfTestData.unembeddedFontPdf(pages)
        val jpeg = PdfTestData.cmykJpeg()
        val fixture = fixture("EL", attachment = original,
            extraAttachments = listOf(MediaType.IMAGE_JPEG_VALUE to jpeg))

        approve(fixture)

        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val job = jobs.findByKey(key(fixture))!!
            assertQueuedDocumentsMatchSources(job, request)
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val archived = loader.load(job.asiakirjat.single { document -> document.asiakirjatyyppi == RecordType.LIITE }.toDescriptor()).sisalto
            assertAttachmentPdf(archived, pages, totalPages = pages + 1,
                prefix = if (embeddedFont) "Embedded page" else "Attachment page")
            PdfDocument(PdfReader(ByteArrayInputStream(archived))).use { pdf ->
                val images = pdf.getPage(pages + 1).resources.getResource(PdfName.XObject)
                val image = images.getAsStream(images.keySet().single())
                assertThat(image.getAsName(PdfName.ColorSpace)).isEqualTo(PdfName.DeviceCMYK)
            }
        }
        verify(transport, never()).laheta(any(), any())
    }
}
