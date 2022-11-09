package fi.elsapalvelu.elsa.service

import org.thymeleaf.context.Context
import java.io.ByteArrayOutputStream
import java.sql.Blob

interface PdfService {

    fun luoPdf(
        template: String,
        context: Context,
        outputStream: ByteArrayOutputStream
    )

    fun yhdistaAsiakirjat(asiakirjat: List<Blob?>, outputStream: ByteArrayOutputStream)
}
