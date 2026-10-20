package fi.elsapalvelu.elsa.service.arkistointi.job

enum class ArkistointiAsiakirjaErrorCategory {
    INPUT_DOCUMENT,
    STORED_DOCUMENT,
    PROCESSING_FAILURE
}

/** Only INPUT_DOCUMENT is eligible for a client error; never attach a library exception or filename. */
class ArkistointiAsiakirjaException(
    val error: ArkistointiError,
    val category: ArkistointiAsiakirjaErrorCategory = ArkistointiAsiakirjaErrorCategory.STORED_DOCUMENT
) : RuntimeException(error.code, null) {
    val errorKey: String get() = error.code
}

internal object ArkistointiAsiakirjaErrors {
    fun missing() = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_MISSING",
        "Arkistoitavaa asiakirjaa ei loydy"
    )

    fun empty() = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_EMPTY",
        "Arkistoitava asiakirja on tyhja"
    )

    fun invalid() = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_INVALID",
        "Arkistoitavaa asiakirjaa ei voida kasitella"
    )

    fun notPdfA2b() = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_NOT_PDFA_2B",
        "Arkistoitava asiakirja ei ole PDF/A-2b-muodossa"
    )

    fun checksumMismatch() = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH",
        "Arkistoitavan asiakirjan tarkistussumma ei vastaa tallennettua arvoa"
    )

    fun processingFailed() = ArkistointiError.create(
        "ARCHIVE_DOCUMENT_PROCESSING_FAILED", "Arkistoitavan asiakirjan kasittely epaonnistui"
    )
}
