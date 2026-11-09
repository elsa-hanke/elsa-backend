package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiExecutionLock
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.StaleArkistointiExecutionException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.EnableTransactionManagement

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DynastyStepLedgerIT.Config::class])
class DynastyStepLedgerIT {
    @Autowired private lateinit var ledger: DynastyStepLedger
    @Autowired private lateinit var properties: ApplicationProperties
    @Autowired private lateinit var jdbc: JdbcTemplate
    private val stepFixture get() = DynastyStepJdbcFixture(jdbc)
    private val grace get() = properties.getArkistointi().getTurku().dynasty.inProgressGrace

    @Configuration
    @EnableTransactionManagement
    @Import(DynastyStepLedger::class, ArkistointiExecutionLock::class)
    class Config {
        @Bean fun properties() = ApplicationProperties()
        @Bean fun dataSource() = object : DriverManagerDataSource(
            "jdbc:tc:postgresql:16.9:///dynastyLedger?TC_DAEMON=true", "test", "test"
        ) {
            override fun getConnection(): java.sql.Connection = super.getConnection().also { connection ->
                connection.createStatement().use { it.execute("set time zone 'Pacific/Honolulu'") }
            }
        }
        @Bean fun transactionManager() = DataSourceTransactionManager(dataSource())
        @Bean fun jdbc(): JdbcTemplate = JdbcTemplate(dataSource()).also { j ->
            DynastyStepJdbcFixture(j).createSchema()
        }
    }

    private fun fixture(): ArkistointiJobExecution = stepFixture.execution(
        jobStatus = ArkistointiJobStatus.KASITTELYSSA, picked = true, taskName = "archive"
    )

    @Test
    fun `intent age uses the grace passed by its destination`() {
        val execution = fixture()
        val shortGrace = java.time.Duration.ofMinutes(1)
        val longGrace = java.time.Duration.ofMinutes(5)
        stepFixture.startedStep(execution, "CASE", age = java.time.Duration.ofMinutes(2))
        assertThat(ledger.begin(execution, DynastyStepKey.Case, shortGrace)).isEqualTo(DynastyStepLedger.Decision.Unconfirmed)
        assertThat(ledger.begin(execution, DynastyStepKey.Case, longGrace)).isEqualTo(DynastyStepLedger.Decision.InProgress)
    }

    @Test
    fun `intent is committed and blocks duplicate create after recovery`() {
        val e = fixture()
        assertThat(ledger.begin(e, DynastyStepKey.Case, grace)).isEqualTo(DynastyStepLedger.Decision.Proceed)
        jdbc.update("update scheduled_tasks set version=2 where task_instance=?",e.taskInstanceId)
        val recovered = e.copy(version=2)
        assertThat(ledger.begin(recovered, DynastyStepKey.Case, grace)).isEqualTo(DynastyStepLedger.Decision.InProgress)
        assertThat(ledger.complete(e.jobId,DynastyStepKey.Case,e.version,"415")).isTrue()
        assertThat(ledger.begin(recovered, DynastyStepKey.Case, grace)).isEqualTo(DynastyStepLedger.Decision.Done("415"))
        assertThatThrownBy { ledger.begin(e, DynastyStepKey.Task, grace) }.isInstanceOf(StaleArkistointiExecutionException::class.java)
        assertThatThrownBy { jdbc.update("delete from arkistointi_dynasty_step where job_id=?",e.jobId) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
    }

    @Test
    fun `expired intent is unconfirmed and concurrent attempts admit only one sender`() {
        val e = fixture()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val results = pool.invokeAll(List(2) {
                java.util.concurrent.Callable { ledger.begin(e, DynastyStepKey.Case, grace) }
            }).map { it.get() }
            assertThat(results).containsExactlyInAnyOrder(DynastyStepLedger.Decision.Proceed,DynastyStepLedger.Decision.InProgress)
            val config = properties.getArkistointi().getTurku().dynasty
            val previous = config.inProgressGrace
            try {
                config.inProgressGrace = java.time.Duration.ZERO
                assertThat(ledger.begin(e, DynastyStepKey.Case, grace)).isEqualTo(DynastyStepLedger.Decision.Unconfirmed)
            } finally { config.inProgressGrace = previous }
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `database age classifies fresh and expired intents in a non UTC session`() {
        assertThat(jdbc.queryForObject("show TimeZone", String::class.java)).isEqualTo("Pacific/Honolulu")
        val e = fixture()
        stepFixture.startedStep(e, "CASE", age = java.time.Duration.ofMinutes(6))
        stepFixture.startedStep(e, "TASK", age = java.time.Duration.ZERO)
        assertThat(ledger.begin(e, DynastyStepKey.Case, grace)).isEqualTo(DynastyStepLedger.Decision.Unconfirmed)
        assertThat(ledger.begin(e, DynastyStepKey.Task, grace)).isEqualTo(DynastyStepLedger.Decision.InProgress)
    }

    @Test
    fun `ledger timestamps store absolute instants generated by the database`() {
        assertThat(jdbc.queryForList(
            "select udt_name from information_schema.columns where table_name='arkistointi_dynasty_step' " +
                "and column_name in ('started_at','completed_at')", String::class.java
        )).containsExactlyInAnyOrder("timestamptz", "timestamptz")
        val e = fixture()
        ledger.begin(e, DynastyStepKey.Case, grace)
        assertThat(ledger.complete(e.jobId, DynastyStepKey.Case, e.version, "415")).isTrue()
        assertThat(jdbc.queryForObject(
            "select started_at <= completed_at and completed_at <= clock_timestamp() " +
                "and started_at > clock_timestamp() - interval '1 minute' from arkistointi_dynasty_step where job_id=?",
            Boolean::class.javaObjectType, e.jobId
        )).isTrue()
    }

    @Test
    fun `original attempt cannot complete a replacement`() {
        val e = fixture()
        ledger.begin(e, DynastyStepKey.Case, grace)
        ledger.abandon(e.jobId,DynastyStepKey.Case,e.version)
        jdbc.update("update scheduled_tasks set version=2 where task_instance=?",e.taskInstanceId)
        ledger.begin(e.copy(version=2), DynastyStepKey.Case, grace)
        assertThat(ledger.complete(e.jobId,DynastyStepKey.Case,1,"415")).isFalse()
        assertThat(ledger.complete(e.jobId,DynastyStepKey.Case,2,"416")).isTrue()
    }
}
