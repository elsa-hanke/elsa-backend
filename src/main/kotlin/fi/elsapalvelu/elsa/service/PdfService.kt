package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Asiakirja
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
}
