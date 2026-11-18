package fi.elsapalvelu.elsa.config

import java.net.URI
import java.net.URISyntaxException

internal object StartupLogDetails {
    private val databasePath = Regex("/[A-Za-z0-9_.-]+")

    // Only emit known address components; never fall back to the original JDBC URL.
    fun databaseAddress(jdbcUrl: String?): String {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) return "unavailable"

        return try {
            val uri = URI(jdbcUrl.removePrefix("jdbc:").substringBefore('?'))
            val host = uri.host ?: return "unavailable"
            if (!databasePath.matches(uri.rawPath.orEmpty()) || uri.fragment != null) return "unavailable"
            val port = if (uri.port == -1) 5432 else uri.port
            if (port !in 1..65535) return "unavailable"
            "host=$host, port=$port, database=${uri.rawPath.removePrefix("/")}"
        } catch (_: URISyntaxException) {
            "unavailable"
        }
    }
}
