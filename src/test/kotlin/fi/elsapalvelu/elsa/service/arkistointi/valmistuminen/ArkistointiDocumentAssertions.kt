package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataBuilder
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkeMetadataRequest
import fi.elsapalvelu.elsa.service.arkistointi.sahke.SahkePakettiBuilder
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.zip.ZipFile

/** The async path freezes the very same generated output that the sync path packages. */
internal fun assertZipContents(
    job: ArkistointiJob, request: Valmistumispyynto, expectedTypes: List<RecordType>, properties: ApplicationProperties
) {
    val configuration = ArkistointiConfigurationProvider(properties)
        .getConfiguration(job.university!!, CaseType.VALMISTUMINEN)
    val records = job.asiakirjat.map { RecordProperties(it.asiakirja!!, it.asiakirjatyyppi!!) }
    val metadata = SahkeMetadataBuilder().build(
        SahkeMetadataRequest(request.opintooikeus, records, request.id.toString(), null, null, null, LocalDate.now()),
        configuration
    )
    val archive = SahkePakettiBuilder().build(metadata, request.opintooikeus, true)
    try {
        ZipFile(archive.zipFilePath).use { zip ->
            val originals = expectedTypes.map { type ->
                when (type) {
                    RecordType.YHTEENVETO -> request.yhteenvetoAsiakirja!!
                    RecordType.LIITE -> request.liitteetAsiakirja!!
                    else -> error("Unexpected sync document type")
                }
            }
            assertThat(zip.entries().asSequence().map { it.name }.toList())
                .containsExactly("sahke.xml", *originals.map { "pdf/${it.nimi}" }.toTypedArray())
            originals.forEach { original ->
                assertThat(zip.getInputStream(zip.getEntry("pdf/${original.nimi}")).use { it.readBytes() })
                    .containsExactly(*original.asiakirjaData!!.data!!)
            }
            assertThat(metadata.asiakirjat.map { it.type }).containsExactlyElementsOf(expectedTypes)
        }
    } finally {
        Files.deleteIfExists(Path.of(archive.zipFilePath))
    }
}

internal fun assertAttachmentPdf(bytes: ByteArray, textPages: Int, totalPages: Int = textPages) {
    PdfDocument(PdfReader(ByteArrayInputStream(bytes))).use { pdf ->
        assertThat(pdf.numberOfPages).isEqualTo(totalPages)
        repeat(textPages) { index ->
            assertThat(PdfTextExtractor.getTextFromPage(pdf.getPage(index + 1))).isEqualTo("Attachment page ${index + 1}")
        }
    }
}

internal fun assertGeneratedSummary(request: Valmistumispyynto) {
    PdfDocument(PdfReader(ByteArrayInputStream(request.yhteenvetoAsiakirja!!.asiakirjaData!!.data!!))).use { pdf ->
        assertThat(PdfTextExtractor.getTextFromPage(pdf.firstPage)).containsIgnoringCase("john")
    }
}
