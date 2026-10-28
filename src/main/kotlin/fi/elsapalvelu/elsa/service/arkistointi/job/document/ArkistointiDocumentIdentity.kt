package fi.elsapalvelu.elsa.service.arkistointi.job.document

import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType

/** Approval-time identity: load bytes by snapshotDocumentId, use sourceDocumentId as the legacy XML Record.nativeId. */
data class ArkistointiDocumentIdentity(
    val sourceDocumentId: Long,
    val snapshotDocumentId: Long,
    val recordType: RecordType,
    val jarjestysnumero: Int
)
