package fi.elsapalvelu.elsa.service.arkistointi.http

import okhttp3.Call
import okhttp3.EventListener
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException

/** One listener per call. Once headers may have been written, delivery cannot be ruled out. */
internal class ArchiveHttpRequestProgress : EventListener() {
    private val started = AtomicBoolean()

    override fun requestHeadersStart(call: Call) {
        started.set(true)
    }

    fun failure(exception: IOException): ArchiveHttpFailure = when {
        started.get() -> ArchiveHttpFailure.DELIVERY_UNCONFIRMED
        exception is SSLException -> ArchiveHttpFailure.TLS_FAILED
        else -> ArchiveHttpFailure.CONNECTION_FAILED
    }
}

internal enum class ArchiveHttpFailure(val retryable: Boolean, val definitelyNotSent: Boolean) {
    CONNECTION_FAILED(true, true),
    TLS_FAILED(false, true),
    DELIVERY_UNCONFIRMED(false, false)
}
