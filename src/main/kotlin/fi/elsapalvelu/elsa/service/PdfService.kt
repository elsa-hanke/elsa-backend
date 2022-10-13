package fi.elsapalvelu.elsa.service

import org.thymeleaf.context.Context
import java.io.ByteArrayOutputStream

interface PdfService {

    fun luoPdf(
        template: String,
        context: Context,
        outputStream: ByteArrayOutputStream
    )
}
