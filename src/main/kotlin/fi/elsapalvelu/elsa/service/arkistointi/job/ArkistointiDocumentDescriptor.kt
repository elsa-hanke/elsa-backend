package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

data class ArkistointiDocumentDescriptor(
    val asiakirjaId: Long,
    val asiakirjatyyppi: RecordType,
    val jarjestysnumero: Int,
    val filename: String,
    val contentType: String,
    val sha256: String
) {
    override fun toString() = "ArkistointiDocumentDescriptor(asiakirjaId=$asiakirjaId)"
}

/** Must be called while the job reference is attached; no entity escapes the transaction. */
internal fun ArkistointiJobAsiakirja.toDescriptor() = ArkistointiDocumentDescriptor(
    asiakirja?.id ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.missing()),
    asiakirjatyyppi.required(), jarjestysnumero.required(), filename.required(), contentType.required(),
    sha256 ?: throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.checksumMismatch())
)
