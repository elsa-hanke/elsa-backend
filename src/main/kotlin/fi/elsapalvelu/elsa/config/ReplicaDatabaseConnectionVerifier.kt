package fi.elsapalvelu.elsa.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Fails replica-mode startup immediately when the tunnel, database, or credentials are invalid.
 * Without this check Hibernate can log the connection failure and still start the web server.
 */
@Component
@Profile("replica")
class ReplicaDatabaseConnectionVerifier(
    private val dataSource: DataSource
) {

    @PostConstruct
    fun verifyConnection() {
        dataSource.connection.use { connection ->
            check(connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS)) {
                "Replica database connection validation failed"
            }
        }
    }

    companion object {
        private const val CONNECTION_VALIDATION_TIMEOUT_SECONDS = 5
    }
}
