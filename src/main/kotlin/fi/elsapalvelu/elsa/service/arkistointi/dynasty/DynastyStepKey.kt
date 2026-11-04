package fi.elsapalvelu.elsa.service.arkistointi.dynasty

/** Stable ledger keys: these values are also used by operator SQL scripts. */
sealed class DynastyStepKey(val databaseKey: String) {
    data object Case : DynastyStepKey("CASE")
    data object Task : DynastyStepKey("TASK")
    data object ArchiveCase : DynastyStepKey("ARCHIVE_CASE")
    data class Document(val order: Int) : DynastyStepKey("DOCUMENT:$order") {
        init {
            require(order >= 0) { "Document order must be nonnegative" }
        }
    }
}
