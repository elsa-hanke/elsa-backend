package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import java.util.UUID

/** IDs keep their validated wire representation, including numeric padding and UUID letter case. */
@JvmInline
value class DynastyCaseId(val value: String) {
    init { dynastyNumericId(value) }
    constructor(value: Long) : this(value.toString())
    val numericValue: Long get() = value.toLong()
    override fun toString() = value
}

@JvmInline
value class DynastyDocumentId(val value: String) {
    init { dynastyNumericId(value) }
    constructor(value: Long) : this(value.toString())
    val numericValue: Long get() = value.toLong()
    override fun toString() = value
}

@JvmInline
value class DynastyTaskId(val value: String) {
    init {
        val uuid = runCatching { UUID.fromString(value) }.getOrNull()
        if (uuid == null || !uuid.toString().equals(value, true)) throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
    }
    constructor(value: UUID) : this(value.toString())
    override fun toString() = value
}

internal fun dynastyNumericId(id: String): Long {
    if (id.isEmpty() || !id.all { it.isDigit() }) throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
    return id.toLongOrNull()?.takeIf { it > 0 } ?: throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
}

internal fun requireDynastyId(id: String?): String = id ?: throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
