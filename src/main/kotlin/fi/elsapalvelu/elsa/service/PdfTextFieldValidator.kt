package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PdfTextFieldValidator(
    private val pdfTextValidator: PdfTextValidator
) {
    fun validate(
        fields: Iterable<Pair<String, String?>>,
        pdfSource: String? = null,
        sourceId: Long? = null,
        sourceDate: LocalDate? = null,
        seurantajaksoId: Long? = null,
        seurantajaksoStartDate: LocalDate? = null
    ) {
        fields.forEach { (field, text) ->
            val sanitizedText = text?.let(PdfTextSanitizer::sanitize)
            val unsupportedCharacters = pdfTextValidator.findUnsupportedCharacters(sanitizedText)
            if (unsupportedCharacters.isNotEmpty()) {
                throw UnsupportedPdfCharactersException(
                    field = field,
                    unsupportedCharacters = unsupportedCharacters.map { it.toString() },
                    seurantajaksoId = seurantajaksoId,
                    seurantajaksoStartDate = seurantajaksoStartDate,
                    pdfSource = pdfSource,
                    sourceId = sourceId,
                    sourceDate = sourceDate
                )
            }
        }
    }
}
