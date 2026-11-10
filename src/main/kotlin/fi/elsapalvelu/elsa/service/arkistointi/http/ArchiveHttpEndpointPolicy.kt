package fi.elsapalvelu.elsa.service.arkistointi.http

/** Transport policy for parsed archive endpoints. Never resolve hostnames to permit test HTTP. */
internal object ArchiveHttpEndpointPolicy {
    fun isAllowed(scheme: String?, host: String?, allowLoopbackHttpForTests: Boolean): Boolean =
        !host.isNullOrBlank() && (scheme == "https" ||
            (allowLoopbackHttpForTests && scheme == "http" && host in setOf("127.0.0.1", "::1", "[::1]")))
}
