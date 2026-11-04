package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import okhttp3.Response

/** One result contract for HTTP responses and completed ledger steps. */
sealed class DynastyResponseKind<T>(val accept: String = "text/plain") {
    abstract fun fromSaved(id: String?): T
    abstract fun toSaved(value: T): String?

    open fun read(response: Response): T {
        val raw = response.peekBody(1025).string()
        if (raw.length > 1024) throw DynastyFailure(DynastyFailureCategory.RESPONSE_INVALID)
        return fromSaved(raw.trim().removeSurrounding("\""))
    }

    data object CaseId : DynastyResponseKind<DynastyCaseId>() {
        override fun fromSaved(id: String?) = DynastyCaseId(requireDynastyId(id))
        override fun toSaved(value: DynastyCaseId) = value.toString()
    }

    data object TaskId : DynastyResponseKind<DynastyTaskId>() {
        override fun fromSaved(id: String?) = DynastyTaskId(requireDynastyId(id))
        override fun toSaved(value: DynastyTaskId) = value.toString()
    }

    data object DocumentId : DynastyResponseKind<DynastyDocumentId>() {
        override fun fromSaved(id: String?) = DynastyDocumentId(requireDynastyId(id))
        override fun toSaved(value: DynastyDocumentId) = value.toString()
    }

    data object NoContent : DynastyResponseKind<Unit>("application/json") {
        override fun read(response: Response) = Unit
        override fun fromSaved(id: String?) = Unit
        override fun toSaved(value: Unit): String? = null
    }
}
