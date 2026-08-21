package fi.elsapalvelu.elsa.web.rest.errors

import fi.elsapalvelu.elsa.web.rest.VALMISTUMISPYYNTO_ENTITY_NAME
import java.time.LocalDate

enum class InvalidPdfAttachmentSource(val messageKey: String) {
    ARVIOINTI("arviointi"),
    ITSEARVIOINTI("itsearviointi"),
    MOTIVAATIOKIRJE("motivaatiokirje"),
    TYOSKENTELYJAKSO("tyoskentelyjakso")
}

class InvalidPdfAttachmentException(
    val attachmentId: Long?,
    attachmentName: String?,
    source: InvalidPdfAttachmentSource,
    val attachmentDate: LocalDate? = null,
    cause: Throwable? = null
) : BadRequestAlertException(
    defaultMessage = "Asiakirja $attachmentId ('$attachmentName') ei ole kelvollinen PDF " +
        "(${source.name}, $attachmentDate).",
    entityName = VALMISTUMISPYYNTO_ENTITY_NAME,
    errorKey = ERROR_KEY
) {
    val attachmentName: String = attachmentName.orEmpty()
    val attachmentSource: String = source.messageKey

    init {
        cause?.let(::initCause)
    }

    companion object {
        const val ERROR_KEY =
            "dataillegal.valmistumispyynnon-liite-ei-ole-kelvollinen-pdf"
    }
}
