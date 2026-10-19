package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.ArchiveImageTestData
import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.PdfCachingResourceRetriever
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.PdfServiceImpl
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfServiceImplMergeTest {
    private val pdfA2bService = PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))
    private val service = PdfServiceImpl(
        templateEngine = mock<SpringTemplateEngine>(),
        pdfMetrics = PdfGenerationMetricsService(SimpleMeterRegistry()),
        pdfContentValidator = PdfContentValidator(),
        pdfTextFieldValidator = mock<PdfTextFieldValidator>(),
        resourceRetriever = PdfCachingResourceRetriever(),
        pdfA2bService = pdfA2bService
    )

    @Test
    fun `merging a long unembedded font attachment preserves every page and its text`() {
        val original = PdfTestData.unembeddedFontPdf(30)
        val output = ByteArrayOutputStream()

        service.yhdistaAsiakirjat(listOf(attachment(original, MediaType.APPLICATION_PDF_VALUE)), output)

        assertPagesAndText(output.toByteArray(), 30)
    }

    @Test
    fun `appending a long unembedded font PDF preserves text without rasterizing either source`() {
        val output = ByteArrayOutputStream()

        service.yhdistaPdf(
            ByteArrayInputStream(PdfTestData.unembeddedFontPdf(30)),
            ByteArrayInputStream(PdfTestData.unembeddedFontPdf(1)), output
        )

        assertPagesAndText(output.toByteArray(), 30, totalPages = 31)
        PdfDocument(PdfReader(ByteArrayInputStream(output.toByteArray()))).use {
            assertThat(PdfTextExtractor.getTextFromPage(it.getPage(31))).isEqualTo("Attachment page 1")
        }
    }

    @Test
    fun `merging a CMYK JPEG retains the image without requiring an RGB PDF-A output intent`() {
        val jpeg = PdfTestData.cmykJpeg()
        assertThat(ImageDataFactory.create(jpeg).colorEncodingComponentsNumber).isEqualTo(4)
        val output = ByteArrayOutputStream()

        service.yhdistaAsiakirjat(listOf(attachment(jpeg, MediaType.IMAGE_JPEG_VALUE)), output)

        PdfDocument(PdfReader(ByteArrayInputStream(output.toByteArray()))).use {
            assertThat(it.numberOfPages).isEqualTo(1)
            val images = it.firstPage.resources.getResource(PdfName.XObject)
            assertThat(images.size()).isEqualTo(1)
            val image = images.getAsStream(images.keySet().single())
            assertThat(image.getAsName(PdfName.Subtype)).isEqualTo(PdfName.Image)
            assertThat(image.getAsName(PdfName.ColorSpace)).isEqualTo(PdfName.DeviceCMYK)
            assertThat(image.getAsNumber(PdfName.Width).intValue()).isEqualTo(8)
        }
    }

    @Test
    fun `merging includes a JPEG uploaded with the image jpg alias`() {
        val output = ByteArrayOutputStream()
        service.yhdistaAsiakirjat(listOf(
            attachment(PdfTestData.certificate(), MediaType.APPLICATION_PDF_VALUE),
            attachment(ArchiveImageTestData.image(), "image/jpg")
        ), output)
        PdfDocument(PdfReader(ByteArrayInputStream(output.toByteArray()))).use {
            assertThat(it.numberOfPages).isEqualTo(2)
            assertThat(it.getPage(2).resources.getResource(PdfName.XObject).size()).isEqualTo(1)
        }
    }

    @Test
    fun `ordinary merged attachments can be normalized separately for an archive snapshot`() {
        val output = ByteArrayOutputStream()
        service.yhdistaAsiakirjat(
            listOf(
                attachment(PdfTestData.unembeddedFontPdf(1), MediaType.APPLICATION_PDF_VALUE),
                attachment(PdfTestData.cmykJpeg(), MediaType.IMAGE_JPEG_VALUE)
            ), output
        )

        val repository = mock<AsiakirjaRepository>()
        whenever(repository.save(any<Asiakirja>())).thenAnswer { invocation ->
            invocation.getArgument<Asiakirja>(0).apply { id = 2L }
        }
        val source = attachment(output.toByteArray(), MediaType.APPLICATION_PDF_VALUE).apply {
            opintooikeus = Opintooikeus(id = 10L)
        }
        val reference = ArkistointiAsiakirjaSnapshotService(repository, pdfA2bService)
            .createReference(source, RecordType.LIITE, 0)
        val snapshot = reference.asiakirja.asiakirjaData!!.data!!

        assertThat(pdfA2bService.isPdfA2b(snapshot)).isTrue
        assertThat(reference.asiakirja.archiveSnapshot).isTrue
        assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(snapshot))
        assertThat(source.asiakirjaData!!.data).containsExactly(*output.toByteArray())
        PdfDocument(PdfReader(ByteArrayInputStream(snapshot))).use {
            assertThat(it.numberOfPages).isEqualTo(2)
        }
    }

    private fun attachment(data: ByteArray, contentType: String) = Asiakirja(
        id = 1L, nimi = "attachment", tyyppi = contentType, asiakirjaData = AsiakirjaData(data = data)
    )

    private fun assertPagesAndText(data: ByteArray, textPages: Int, totalPages: Int = textPages) {
        PdfDocument(PdfReader(ByteArrayInputStream(data))).use { document ->
            assertThat(document.numberOfPages).isEqualTo(totalPages)
            repeat(textPages) { index ->
                assertThat(PdfTextExtractor.getTextFromPage(document.getPage(index + 1)))
                    .isEqualTo("Attachment page ${index + 1}")
            }
        }
    }
}
