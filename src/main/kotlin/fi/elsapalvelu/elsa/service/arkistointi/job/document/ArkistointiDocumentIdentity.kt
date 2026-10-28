package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

data class ArkistointiDocumentIdentity(
    val sourceDocumentId: Long,
    val snapshotDocumentId: Long,
    val recordType: RecordType,
    val jarjestysnumero: Int
)
