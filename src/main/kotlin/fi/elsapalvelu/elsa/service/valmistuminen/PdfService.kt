package fi.elsapalvelu.elsa.service.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.impl.valmistuminen.PdfKooste
import org.thymeleaf.context.Context
import java.io.InputStream
import java.io.OutputStream

interface PdfService {

    fun luoPdf(
        template: String,
        context: Context,
        outputStream: OutputStream
    )

    fun yhdistaAsiakirjat(asiakirjat: List<Asiakirja>, outputStream: OutputStream)

    fun yhdistaPdf(source: InputStream, newPdf: InputStream, outputStream: OutputStream)

    /**
     * Avaa koonnin, johon dokumentteja voi lisata ilman etta jo koottua osuutta kirjoitetaan
     * uudelleen. Kutsujan vastuulla on sulkea kooste (esim. [PdfKooste.valmis]).
     */
    fun avaaKooste(ensimmainenDokumentti: ByteArray): PdfKooste
}
