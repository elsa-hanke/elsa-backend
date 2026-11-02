package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import java.util.UUID

/** Validate IDs without changing their representation; only the HTTP reader unwraps responses. */
internal object DynastyId {
    fun requireValid(id: String?, guid: Boolean): String {
        if (id == null) throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
        val valid = if (guid) {
            runCatching { UUID.fromString(id).toString().equals(id, true) }.getOrDefault(false)
        } else {
            id.all { it.isDigit() } && (id.toLongOrNull() ?: 0) > 0
        }
        if (!valid) throw DynastyFailure("DYNASTY_RESPONSE_INVALID")
        return id
    }
}
