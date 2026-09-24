package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.utils.PdfMerger
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_YHDISTA_PDF
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable

/**
 * Kokoaa valmistumispyynnon liitedokumentit yhteen PDF-dokumenttiin.
 *
 * Aiemmin jokainen lisays luki ja kirjoitti koko siihenastisen dokumentin uudelleen, jolloin
 * tyomaara kasvoi neliollisesti merkintojen maaraan nahden (5092 suoritemerkintaa = n. 13 min
 * pelkkaa yhdistelya kehityskoneella). Tassa kohdedokumentti pidetaan auki koko koonnin ajan,
 * jolloin jokainen lisays maksaa saman verran riippumatta siita kuinka monta sivua on jo koossa.
 *
 * [perinteinenYhdistely] palauttaa vanhan, neliollisesti kasvavan toteutuksen. Se on tarkoitettu
 * vain suorituskykymittauksiin (A/B-vertailu samalla aineistolla) eika sita pida ottaa kayttoon
 * tuotannossa.
 *
 * [alykasYhdistely] ottaa kayttoon iTextin "smart mode" -tilan. Jokainen suoritemerkinta
 * renderoidaan omaksi PDF/A-dokumentikseen, joten jokainen niista sisaltaa oman kopionsa
 * fonttien osajoukoista, varioprofiilista ja metatiedoista. Ilman tata tilaa yhdistely kopioi
 * ne kaikki erikseen (mitattu 31 KiB/sivu), sen kanssa samanlaiset objektit jaetaan
 * (mitattu 4 KiB/sivu). Tama pienentaa tallennettavan asiakirjan noin kahdeksasosaan, mika
 * lyhentaa merkittavasti tietokantaan tallennusta - koonnin jalkeen se on tyon suurin vaihe.
 */
class PdfKooste(
    ensimmainenDokumentti: ByteArray,
    private val pdfMetrics: PdfGenerationMetricsService,
    private val perinteinenYhdistely: Boolean = false,
    alykasYhdistely: Boolean = true
) : Closeable {

    private val outputStream = ByteArrayOutputStream()
    private val kohde: PdfDocument? =
        if (perinteinenYhdistely) null
        else PdfDocument(
            PdfReader(ByteArrayInputStream(ensimmainenDokumentti)),
            if (alykasYhdistely) PdfWriter(outputStream, WriterProperties().useSmartMode())
            else PdfWriter(outputStream)
        )
    private val merger = kohde?.let { PdfMerger(it) }

    /** Kaytossa vain perinteisessa tilassa. */
    private var kootutTavut: ByteArray = ensimmainenDokumentti
    private var perinteisenSivumaara: Int =
        if (perinteinenYhdistely) sivumaara(ensimmainenDokumentti) else 0

    private var suljettu = false

    val sivuja: Int
        get() = kohde?.numberOfPages ?: perinteisenSivumaara

    fun lisaa(pdf: ByteArray) {
        pdfMetrics.trackOperation(OP_YHDISTA_PDF) {
            if (perinteinenYhdistely) {
                yhdistaUudelleenkirjoittamalla(pdf)
            } else {
                PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { lahde ->
                    val pdfMerger = merger ?: error("PDF-koosteen yhdistin puuttuu")
                    pdfMerger.merge(lahde, 1, lahde.numberOfPages)
                }
            }
        }
    }

    fun lisaa(pdf: ByteArrayOutputStream) = lisaa(pdf.toByteArray())

    /**
     * Sulkee kohdedokumentin ja palauttaa valmiin PDF:n tavuina.
     */
    fun valmis(): ByteArray {
        close()
        return if (perinteinenYhdistely) kootutTavut else outputStream.toByteArray()
    }

    override fun close() {
        if (!suljettu) {
            suljettu = true
            kohde?.close()
        }
    }

    /** Vanha toteutus: koko siihenastinen dokumentti luetaan ja kirjoitetaan uudelleen. */
    private fun yhdistaUudelleenkirjoittamalla(pdf: ByteArray) {
        val uusiSisalto = ByteArrayOutputStream()
        val tulos = PdfDocument(PdfReader(ByteArrayInputStream(kootutTavut)), PdfWriter(uusiSisalto))
        val uusiMerger = PdfMerger(tulos)
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { lahde ->
            uusiMerger.merge(lahde, 1, lahde.numberOfPages)
        }
        perinteisenSivumaara = tulos.numberOfPages
        tulos.close()
        kootutTavut = uusiSisalto.toByteArray()
    }

    private fun sivumaara(pdf: ByteArray): Int =
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { it.numberOfPages }
}

