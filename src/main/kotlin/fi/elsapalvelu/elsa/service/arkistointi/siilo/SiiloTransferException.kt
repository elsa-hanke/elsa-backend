package fi.elsapalvelu.elsa.service.arkistointi.siilo

/** Safe diagnostics only; never retain a URL, response body, filename or original cause. */
class SiiloTransferException(
    val code: String,
    val retryable: Boolean,
    val httpStatus: Int? = null,
    val category: String? = null
) : RuntimeException("$code [status=$httpStatus, category=$category]")

/** Retain only a fixed category, never exception messages or causes. */
internal fun siiloIoCategory(exception: java.io.IOException): String = when (exception) {
    is java.net.SocketTimeoutException, is java.io.InterruptedIOException -> "TIMEOUT"
    is java.net.UnknownHostException -> "DNS"
    is java.net.ConnectException -> "CONNECT"
    is javax.net.ssl.SSLException -> "TLS"
    else -> "IO"
}
