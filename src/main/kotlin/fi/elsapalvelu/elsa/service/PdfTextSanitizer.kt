package fi.elsapalvelu.elsa.service

object PdfTextSanitizer {
    private val puaRegex = Regex("[\uE000-\uF8FF]")

    fun sanitize(input: String): String =
        input.replace("\uF0B7", "\u2022")
            .replace("\uF0A7", "\u25E6")
            .replace(puaRegex, "")
}
