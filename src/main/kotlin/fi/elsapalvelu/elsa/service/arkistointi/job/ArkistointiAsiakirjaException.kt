package fi.elsapalvelu.elsa.service.arkistointi.job

class ArkistointiAsiakirjaException(
    val error: ArkistointiError,
    cause: Throwable? = null
) : RuntimeException(error.description, cause)

internal object ArkistointiAsiakirjaErrors {
    fun missing(identifier: String) = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_MISSING",
        "Arkistoitavaa asiakirjaa '$identifier' ei loydy"
    )

    fun empty(identifier: String) = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_EMPTY",
        "Arkistoitava asiakirja '$identifier' on tyhja"
    )

    fun invalid(identifier: String) = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_INVALID",
        "Arkistoitavaa asiakirjaa '$identifier' ei voida kasitella"
    )

    fun notPdfA2b(identifier: String) = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_NOT_PDFA_2B",
        "Arkistoitava asiakirja '$identifier' ei ole PDF/A-2b-muodossa"
    )

    fun checksumMismatch(identifier: String) = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH",
        "Arkistoitavan asiakirjan '$identifier' tarkistussumma ei vastaa tallennettua arvoa"
    )
}
