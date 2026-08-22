package fi.elsapalvelu.elsa.web.rest.errors

import java.time.LocalDate

class UnsupportedPdfCharactersException(
    val field: String,
    val unsupportedCharacters: List<String>,
    val seurantajaksoId: Long?,
    val seurantajaksoStartDate: LocalDate?
) : BadRequestAlertException(
    defaultMessage = "Seurantajakson kenttä '$field' sisältää PDF-fonteista puuttuvia merkkejä: " +
        unsupportedCharacters.joinToString(", "),
    entityName = "seurantajakso",
    errorKey = ERROR_KEY
) {
    companion object {
        const val ERROR_KEY = "dataillegal.pdf-tiedostossa-tukemattomia-merkkeja"
    }
}
