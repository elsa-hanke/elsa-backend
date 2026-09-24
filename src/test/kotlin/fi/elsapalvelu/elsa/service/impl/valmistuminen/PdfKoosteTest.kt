package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfKoosteTest {

    private val pdfMetrics = PdfGenerationMetricsService(SimpleMeterRegistry())

    @Test
    fun `kokoaa kaikki dokumentit yhdeksi PDF-tiedostoksi oikeassa jarjestyksessa`() {
        val kooste = PdfKooste(pdf("Ensimmainen"), pdfMetrics)
        kooste.lisaa(pdf("Toinen"))
        kooste.lisaa(pdf("Kolmas"))

        val tulos = kooste.valmis()

        assertThat(sivumaara(tulos)).isEqualTo(3)
        assertThat(tekstit(tulos)).containsExactly("Ensimmainen", "Toinen", "Kolmas")
    }

    @Test
    fun `sivumaara kasvaa jokaisen lisayksen myota`() {
        PdfKooste(pdf("Ensimmainen"), pdfMetrics).use { kooste ->
            assertThat(kooste.sivuja).isEqualTo(1)
            kooste.lisaa(pdf("Toinen"))
            assertThat(kooste.sivuja).isEqualTo(2)
        }
    }

    @Test
    fun `tuottaa saman lopputuloksen kuin dokumentti kerrallaan yhdistaminen`() {
        val dokumentit = (1..5).map { pdf("Sivu $it") }

        val kooste = PdfKooste(dokumentit.first(), pdfMetrics)
        dokumentit.drop(1).forEach(kooste::lisaa)
        val koosteenTulos = kooste.valmis()

        var perakkainYhdistetty = dokumentit.first()
        dokumentit.drop(1).forEach { perakkainYhdistetty = yhdistaVanhallaTavalla(perakkainYhdistetty, it) }

        assertThat(sivumaara(koosteenTulos)).isEqualTo(sivumaara(perakkainYhdistetty))
        assertThat(tekstit(koosteenTulos)).isEqualTo(tekstit(perakkainYhdistetty))
    }

    @Test
    fun `valmis on turvallista kutsua vaikka kooste olisi jo suljettu`() {
        val kooste = PdfKooste(pdf("Ensimmainen"), pdfMetrics)
        val tulos = kooste.valmis()

        kooste.close()

        assertThat(sivumaara(tulos)).isEqualTo(1)
    }

    @Test
    fun `mittauskaytossa oleva perinteinen yhdistely tuottaa saman dokumentin`() {
        val dokumentit = (1..5).map { pdf("Sivu $it") }

        val kooste = PdfKooste(dokumentit.first(), pdfMetrics)
        dokumentit.drop(1).forEach(kooste::lisaa)
        val uusiTulos = kooste.valmis()

        val perinteinen = PdfKooste(dokumentit.first(), pdfMetrics, perinteinenYhdistely = true)
        dokumentit.drop(1).forEach(perinteinen::lisaa)
        assertThat(perinteinen.sivuja).isEqualTo(5)
        val perinteinenTulos = perinteinen.valmis()

        assertThat(tekstit(perinteinenTulos)).isEqualTo(tekstit(uusiTulos))
        assertThat(sivumaara(perinteinenTulos)).isEqualTo(sivumaara(uusiTulos))
    }

    @Test
    fun `alykas yhdistely tuottaa saman sisallon kuin ilman sita`() {
        val dokumentit = (1..12).map { pdfFontilla("Sivu $it") }

        val alykas = PdfKooste(dokumentit.first(), pdfMetrics, alykasYhdistely = true)
        dokumentit.drop(1).forEach(alykas::lisaa)
        val alykasTulos = alykas.valmis()

        val tavallinen = PdfKooste(dokumentit.first(), pdfMetrics, alykasYhdistely = false)
        dokumentit.drop(1).forEach(tavallinen::lisaa)
        val tavallinenTulos = tavallinen.valmis()

        assertThat(sivumaara(alykasTulos)).isEqualTo(sivumaara(tavallinenTulos))
        assertThat(tekstit(alykasTulos)).isEqualTo(tekstit(tavallinenTulos))
    }

    @Test
    fun `alykas yhdistely ei kopioi samoja fontteja jokaisesta lahdedokumentista`() {
        val dokumentit = (1..12).map { pdfFontilla("Sivu $it") }

        val alykas = PdfKooste(dokumentit.first(), pdfMetrics, alykasYhdistely = true)
        dokumentit.drop(1).forEach(alykas::lisaa)
        val alykasTulos = alykas.valmis()

        val tavallinen = PdfKooste(dokumentit.first(), pdfMetrics, alykasYhdistely = false)
        dokumentit.drop(1).forEach(tavallinen::lisaa)
        val tavallinenTulos = tavallinen.valmis()

        // Sama sisalto, mutta jaetut objektit: koosteen pitaa olla selvasti pienempi.
        assertThat(alykasTulos.size).isLessThan(tavallinenTulos.size / 2)
    }

    private fun pdf(teksti: String): ByteArray {
        val out = ByteArrayOutputStream()
        Document(PdfDocument(PdfWriter(out))).use { it.add(Paragraph(teksti)) }
        return out.toByteArray()
    }

    /**
     * Dokumentti, johon on upotettu fontti - kuten oikeissa suoritemerkinnoissa. Ilman upotettua
     * fonttia lahdedokumenteissa ei ole mitaan merkittavaa jaettavaa.
     */
    private fun pdfFontilla(teksti: String): ByteArray {
        val out = ByteArrayOutputStream()
        val fontti = PdfFontFactory.createFont(
            FONTTI,
            PdfEncodings.IDENTITY_H,
            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
        )
        // Ilman osajoukkoa jokainen lahdedokumentti sisaltaa koko fontin, kuten oikeissa
        // PDF/A-dokumenteissa on iso maara jaettavaa sisaltoa.
        fontti.setSubset(false)
        Document(PdfDocument(PdfWriter(out))).use {
            it.add(Paragraph(teksti).setFont(fontti))
        }
        return out.toByteArray()
    }

    /** Vanha toteutus: koko siihenastinen dokumentti luettiin ja kirjoitettiin uudelleen. */
    private fun yhdistaVanhallaTavalla(olemassaoleva: ByteArray, uusi: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val tulos = PdfDocument(PdfReader(ByteArrayInputStream(olemassaoleva)), PdfWriter(out))
        val merger = com.itextpdf.kernel.utils.PdfMerger(tulos)
        PdfDocument(PdfReader(ByteArrayInputStream(uusi))).use { lahde ->
            merger.merge(lahde, 1, lahde.numberOfPages)
        }
        tulos.close()
        return out.toByteArray()
    }

    private fun sivumaara(pdf: ByteArray): Int =
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { it.numberOfPages }

    private fun tekstit(pdf: ByteArray): List<String> =
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { doc ->
            (1..doc.numberOfPages).map { sivu ->
                com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
                    .getTextFromPage(doc.getPage(sivu)).trim()
            }
        }

    private companion object {
        const val FONTTI = "src/main/resources/fonts/LiberationSerif-Regular.ttf"
    }
}

