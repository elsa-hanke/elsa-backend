package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.styledxmlparser.resolver.resource.DefaultResourceRetriever
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves the external resources referenced by the PDF templates, currently the FontAwesome
 * stylesheet and its icon webfonts.
 *
 * iText's default implementation re-fetches every resource for every single rendering and applies
 * no timeout at all. With thousands of entries in a graduation request this was measured at around
 * 260 ms of added time per document, and when the remote host did not answer, over 75 seconds per
 * document with no upper bound.
 *
 * Resources are resolved in a fixed order:
 *
 *  1. **Classpath.** Known addresses are mapped to files vendored inside the application itself,
 *     so the common path needs no network at all and documents are reproducible.
 *  2. **Cache.** Any address is resolved at most once per application instance.
 *  3. **Network.** Used only as a last resort, with connect and read timeouts and a size limit.
 *
 * Every fall-through to the network is logged at ERROR, because it means one of two assumptions
 * has broken: either a vendored file is missing from the packaged artifact, or a template now
 * references a resource that has not been vendored. Neither should ever happen in normal
 * operation, but the document is still produced rather than failing the whole request.
 */
@Component
class PdfCachingResourceRetriever : DefaultResourceRetriever() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = ConcurrentHashMap<String, CachedResource>()

    override fun getInputStreamByUrl(url: URL): InputStream? =
        getByteArrayByUrl(url)?.let { ByteArrayInputStream(it) }

    override fun getByteArrayByUrl(url: URL): ByteArray? {
        if (!isRemote(url)) {
            return super.getByteArrayByUrl(url)
        }
        return cache.computeIfAbsent(url.toExternalForm()) { address ->
            CachedResource(resolve(url, address))
        }.data
    }

    /**
     * Classpath first, network last. A failure is cached as well: without that, a host that does
     * not answer would cost the connect and read timeouts again for every entry, which for a
     * request of several thousand entries would take hours.
     */
    private fun resolve(url: URL, address: String): ByteArray? {
        val classpathLocation = localPath(address)
        if (classpathLocation == null) {
            log.error(
                "A PDF template references a remote resource that has not been vendored. It will " +
                    "be downloaded over the network, which is slow and makes the generated " +
                    "documents depend on an external host. The resource should be added to the " +
                    "application's own classpath. [$address]"
            )
        } else {
            readFromClasspath(classpathLocation, address)?.let { return it }
            log.error(
                "A vendored PDF resource is missing from the classpath. This is a packaging " +
                    "fault: the address has a local counterpart configured, but the file is not " +
                    "present in the deployed artifact. Falling back to the network. " +
                    "[$address -> $classpathLocation]"
            )
        }
        return fetch(url, address)
    }

    /**
     * Maps a known remote address to a file on the classpath. The stylesheet refers to its fonts
     * using relative addresses, which iText resolves against the original address, so those match
     * this mapping too and the templates need no changes.
     *
     * Visible for testing so that the path handling can be verified without touching the network.
     */
    internal fun localPath(address: String): String? =
        VENDORED_RESOURCES.entries
            .firstOrNull { address.startsWith(it.key) }
            ?.let { (prefix, directory) ->
                val relative =
                    address.removePrefix(prefix).substringBefore('?').substringBefore('#')
                if (relative.isEmpty() || relative.contains("..")) null else directory + relative
            }

    private fun readFromClasspath(location: String, address: String): ByteArray? {
        val resource = ClassPathResource(location)
        if (!resource.exists()) {
            return null
        }
        return resource.inputStream.use { it.readBytes() }.also {
            log.debug("PDF resource read locally [$address -> $location, ${it.size} bytes]")
        }
    }

    private fun fetch(url: URL, address: String): ByteArray? = try {
        val connection = url.openConnection().apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        val data = connection.getInputStream().use { it.readNBytes(MAX_SIZE_BYTES + 1) }
        if (data.size > MAX_SIZE_BYTES) {
            log.error(
                "A remote PDF resource is too large, the document is produced without it " +
                    "[$address, over $MAX_SIZE_BYTES bytes]"
            )
            null
        } else {
            log.warn("Remote PDF resource downloaded and cached [$address, ${data.size} bytes]")
            data
        }
    } catch (e: Exception) {
        log.error(
            "Downloading a remote PDF resource failed, the document is produced without it. " +
                "Icons or styling may be missing. [$address]: ${e.message}"
        )
        null
    }

    private fun isRemote(url: URL): Boolean {
        val protocol = url.protocol?.lowercase(Locale.ROOT)
        return protocol == "http" || protocol == "https"
    }

    /** Should only be reset in tests. */
    fun clearCache() = cache.clear()

    /** ConcurrentHashMap does not allow null values, so a failed resolution is wrapped too. */
    private class CachedResource(val data: ByteArray?)

    private companion object {
        const val CONNECT_TIMEOUT_MS = 3000
        const val READ_TIMEOUT_MS = 5000
        const val MAX_SIZE_BYTES = 10 * 1024 * 1024

        /** Remote prefix -> classpath directory. Both must end in a slash. */
        val VENDORED_RESOURCES = mapOf(
            "https://use.fontawesome.com/releases/v5.15.4/" to "vendor/fontawesome/5.15.4/"
        )
    }
}

