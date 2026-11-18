package fi.elsapalvelu.elsa.service.arkistointi.job.document

/** Match immutable payload identities to loaded documents independently of transport or list order. */
internal object ArkistointiManifestValidator {
    fun validate(
        identities: List<ArkistointiDocumentIdentity>,
        documents: List<LoadedArkistointiAsiakirja>
    ): Map<Long, ArkistointiDocumentIdentity> {
        require(documents.isNotEmpty() && identities.size == documents.size)
        require(identities.all { it.sourceDocumentId > 0 && it.snapshotDocumentId > 0 && it.jarjestysnumero >= 0 })
        require(identities.map { it.snapshotDocumentId }.distinct().size == identities.size)
        require(identities.map { it.jarjestysnumero }.distinct().size == identities.size)
        require(documents.map { it.asiakirjaId }.distinct().size == documents.size)
        val bySnapshot = identities.associateBy { it.snapshotDocumentId }
        documents.forEach { document ->
            val identity = requireNotNull(bySnapshot[document.asiakirjaId])
            require(identity.recordType == document.asiakirjatyyppi && identity.jarjestysnumero == document.jarjestysnumero)
        }
        return bySnapshot
    }
}
