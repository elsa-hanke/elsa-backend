package fi.elsapalvelu.elsa.web.rest.errors

import java.time.LocalDate

class UnsupportedPdfCharactersException(
    val field: String,
    val unsupportedCharacters: List<String>,
    val seurantajaksoId: Long? = null,
    val seurantajaksoStartDate: LocalDate? = null,
    val pdfSource: String? = null,
    val sourceId: Long? = null,
    val sourceDate: LocalDate? = null
) : BadRequestAlertException(
    defaultMessage = "Kenttä '$field' sisältää PDF-fonteista puuttuvia merkkejä: " +
        unsupportedCharacters.joinToString(", "),
    entityName = pdfSource ?: "pdf",
    errorKey = ERROR_KEY
) {
    companion object {
        const val ERROR_KEY = "dataillegal.pdf-tiedostossa-tukemattomia-merkkeja"
    }
}
