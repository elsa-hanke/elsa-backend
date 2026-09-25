package fi.elsapalvelu.elsa.service.valmistuminen

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.impl.valmistuminen.PdfAssembler
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
     * Opens an assembly that documents can be added to without rewriting the part already
     * assembled. The caller is responsible for closing it (e.g. via [PdfAssembler.finish]).
     */
    fun openAssembler(firstDocument: ByteArray): PdfAssembler
}
