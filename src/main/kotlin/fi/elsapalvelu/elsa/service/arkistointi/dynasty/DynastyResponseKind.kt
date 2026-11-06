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

    /** An identifier saved and read back as its own string; [parse] validates the remote value. */
    class IdKind<T>(private val parse: (String) -> T) : DynastyResponseKind<T>() {
        override fun fromSaved(id: String?) = parse(requireDynastyId(id))
        override fun toSaved(value: T) = value.toString()
    }

    data object NoContent : DynastyResponseKind<Unit>("application/json") {
        override fun read(response: Response) = Unit
        override fun fromSaved(id: String?) = Unit
        override fun toSaved(value: Unit): String? = null
    }

    companion object {
        val CaseId = IdKind { DynastyCaseId(it) }
        val TaskId = IdKind { DynastyTaskId(it) }
        val DocumentId = IdKind { DynastyDocumentId(it) }
    }
}
