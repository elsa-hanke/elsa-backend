package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import java.util.UUID

/** Phantom scopes that keep case and document ids from being mixed up at compile time. */
sealed interface DynastyIdScope {
    data object Case : DynastyIdScope
    data object Document : DynastyIdScope
}

/** IDs keep their validated wire representation, including numeric padding and UUID letter case. */
@JvmInline
value class DynastyNumericId<S : DynastyIdScope>(val value: String) {
    init { dynastyNumericId(value) }
    constructor(value: Long) : this(value.toString())
    val numericValue: Long get() = value.toLong()
    override fun toString() = value
}

typealias DynastyCaseId = DynastyNumericId<DynastyIdScope.Case>
typealias DynastyDocumentId = DynastyNumericId<DynastyIdScope.Document>

@JvmInline
value class DynastyTaskId(val value: String) {
    init {
        val uuid = runCatching { UUID.fromString(value) }.getOrNull()
        if (uuid == null || !uuid.toString().equals(value, true)) throw DynastyFailure(DynastyFailureCategory.RESPONSE_INVALID)
    }
    constructor(value: UUID) : this(value.toString())
    override fun toString() = value
}

internal fun dynastyNumericId(id: String): Long {
    if (id.isEmpty() || !id.all { it.isDigit() }) throw DynastyFailure(DynastyFailureCategory.RESPONSE_INVALID)
    return id.toLongOrNull()?.takeIf { it > 0 } ?: throw DynastyFailure(DynastyFailureCategory.RESPONSE_INVALID)
}

internal fun requireDynastyId(id: String?): String = id ?: throw DynastyFailure(DynastyFailureCategory.RESPONSE_INVALID)
