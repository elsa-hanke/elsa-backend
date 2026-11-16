package fi.elsapalvelu.elsa.service.arkistointi.logging

import java.util.Collections
import java.util.IdentityHashMap

/** Copies types and stack frames without messages that may contain document names, payloads or credentials. */
internal fun archiveLogException(exception: Throwable): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    fun copy(source: Throwable, depth: Int): Throwable {
        val result = RuntimeException(source.javaClass.name)
        result.stackTrace = source.stackTrace
        if (depth < MAX_DEPTH && seen.size < MAX_EXCEPTIONS && seen.add(source)) {
            source.cause?.takeUnless { it in seen }?.let { result.initCause(copy(it, depth + 1)) }
            source.suppressed.take(MAX_EXCEPTIONS).forEach {
                if (it !in seen && seen.size < MAX_EXCEPTIONS) result.addSuppressed(copy(it, depth + 1))
            }
        }
        return result
    }
    return copy(exception, 0)
}

private const val MAX_DEPTH = 16
private const val MAX_EXCEPTIONS = 32
