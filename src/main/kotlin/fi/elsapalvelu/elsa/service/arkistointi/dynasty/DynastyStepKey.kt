package fi.elsapalvelu.elsa.service.arkistointi.dynasty

/** Stable ledger keys: these values are also used by operator SQL scripts. */
sealed class DynastyStepKey<T>(val databaseKey: String, val responseKind: DynastyResponseKind<T>) {
    data object Case : DynastyStepKey<DynastyCaseId>("CASE", DynastyResponseKind.CaseId)
    data object Task : DynastyStepKey<DynastyTaskId>("TASK", DynastyResponseKind.TaskId)
    data object ArchiveCase : DynastyStepKey<Unit>("ARCHIVE_CASE", DynastyResponseKind.NoContent)
    data class Document(val order: Int) : DynastyStepKey<DynastyDocumentId>("DOCUMENT:$order", DynastyResponseKind.DocumentId) {
        init {
            require(order >= 0) { "Document order must be nonnegative" }
        }
    }
}
