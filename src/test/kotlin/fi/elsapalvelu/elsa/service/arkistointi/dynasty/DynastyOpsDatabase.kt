package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import java.nio.file.Path
import java.util.UUID
import javax.sql.DataSource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.Container.ExecResult
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.MountableFile

/** Disposable PostgreSQL with the minimal ledger schema; runs the real operator scripts with psql. */
class DynastyOpsDatabase {
    private val database = PostgreSQLContainer("postgres:16.9")
    lateinit var dataSource: DataSource
        private set
    lateinit var jdbc: JdbcTemplate
        private set

    fun start(): DynastyOpsDatabase {
        database.start()
        dataSource = DriverManagerDataSource(database.jdbcUrl, database.username, database.password)
        jdbc = JdbcTemplate(dataSource)
        DynastyStepJdbcFixture(jdbc).createSchema()
        copyScript("dynasty-resolve-step.sql", RESOLVE_SCRIPT)
        copyScript("retry-job.sql", RETRY_SCRIPT)
        return this
    }

    fun stop() = database.stop()

    fun resolveStep(jobId: UUID, step: String, remote: String, action: String): ExecResult =
        psql(RESOLVE_SCRIPT, "job_id=$jobId", "step_key=$step", "remote_id=$remote", "action=$action")

    fun retryJob(jobId: UUID): ExecResult = psql(RETRY_SCRIPT, "job_id=$jobId")

    private fun copyScript(name: String, target: String) = database.copyFileToContainer(
        MountableFile.forHostPath(Path.of("ops/arkistointi/$name").toAbsolutePath().toString()), target
    )

    private fun psql(script: String, vararg variables: String) = database.execInContainer(
        *(listOf("psql", "-U", database.username, "-d", database.databaseName) +
            variables.flatMap { listOf("-v", it) } + listOf("-f", script)).toTypedArray()
    )

    private companion object {
        const val RESOLVE_SCRIPT = "/tmp/resolve.sql"
        const val RETRY_SCRIPT = "/tmp/retry.sql"
    }
}
