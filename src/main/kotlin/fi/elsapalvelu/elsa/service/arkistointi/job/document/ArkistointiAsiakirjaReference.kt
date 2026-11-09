package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

data class ArkistointiAsiakirjaReference(
    val asiakirja: Asiakirja,
    val asiakirjatyyppi: RecordType,
    val jarjestysnumero: Int,
    val filename: String,
    val contentType: String,
    val sha256: String
) {
    /** The source may itself be an existing snapshot; never infer its ID from the blob ID. */
    fun identity(sourceDocumentId: Long): ArkistointiDocumentIdentity = ArkistointiDocumentIdentity(
        sourceDocumentId = sourceDocumentId,
        snapshotDocumentId = asiakirja.id.required(),
        recordType = asiakirjatyyppi,
        jarjestysnumero = jarjestysnumero
    )
}
