package fi.elsapalvelu.elsa.service.arkistointi.support

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

internal object ArchiveRetryTestSupport {
    /** psql commands are client-side; JDBC uses the test's transaction instead of BEGIN/COMMIT. */
    fun retry(jdbc: JdbcTemplate, transactions: TransactionTemplate, id: UUID) {
        val sql = Files.readString(Path.of("ops/arkistointi/retry-job.sql"))
            .lineSequence().filterNot { it.startsWith("\\") || it == "BEGIN;" || it == "COMMIT;" }
            .joinToString("\n").replace(":'job_id'", "'$id'")
        transactions.executeWithoutResult { jdbc.execute(sql) }
    }
}
