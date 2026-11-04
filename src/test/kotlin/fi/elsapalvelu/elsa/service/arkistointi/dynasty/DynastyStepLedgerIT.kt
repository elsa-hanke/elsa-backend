package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties
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
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DynastyStepLedgerIT.Config::class])
class DynastyStepLedgerIT {
    @Autowired private lateinit var ledger: DynastyStepLedger
    @Autowired private lateinit var properties: ApplicationProperties
    @Autowired private lateinit var jdbc: JdbcTemplate

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
            j.execute("create table scheduled_tasks(task_name text,task_instance text,version bigint,picked boolean,consecutive_failures integer default 0)")
            j.execute("create table arkistointi_job(id uuid primary key,status text)")
            val xml = javaClass.getResourceAsStream("/config/liquibase/changelog/20261101090000_added_arkistointi_dynasty_step.xml")!!
            val sql = xml.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).getElementsByTagName("sql").item(0).textContent }
            j.execute(sql)
        }
    }

    private fun fixture(): ArkistointiJobExecution {
        val e = ArkistointiJobExecution("archive",UUID.randomUUID().toString(),1)
        jdbc.update("insert into scheduled_tasks(task_name,task_instance,version,picked) values (?,?,?,true)",e.taskName,e.taskInstanceId,e.version)
        jdbc.update("insert into arkistointi_job values (?,'KASITTELYSSA')",e.jobId)
        return e
    }

    @Test
    fun `intent age uses the grace passed by its destination`() {
        val execution = fixture()
        val shortGrace = java.time.Duration.ofMinutes(1)
        val longGrace = java.time.Duration.ofMinutes(5)
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) " +
            "values (?,'CASE','ALOITETTU',?,clock_timestamp() - interval '2 minutes')", execution.jobId, execution.version)
        assertThat(ledger.begin(execution, "CASE", shortGrace)).isEqualTo(DynastyStepLedger.Decision.Unconfirmed)
        assertThat(ledger.begin(execution, "CASE", longGrace)).isEqualTo(DynastyStepLedger.Decision.InProgress)
    }

    @Test
    fun `intent is committed and blocks duplicate create after recovery`() {
        val e = fixture()
        assertThat(ledger.begin(e, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)).isEqualTo(DynastyStepLedger.Decision.Proceed)
        jdbc.update("update scheduled_tasks set version=2 where task_instance=?",e.taskInstanceId)
        val recovered = e.copy(version=2)
        assertThat(ledger.begin(recovered, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)).isEqualTo(DynastyStepLedger.Decision.InProgress)
        assertThat(ledger.complete(e.jobId,"CASE",e.version,"415")).isTrue()
        assertThat(ledger.begin(recovered, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)).isEqualTo(DynastyStepLedger.Decision.Done("415"))
        assertThatThrownBy { ledger.begin(e, "TASK", properties.getArkistointi().getTurku().dynasty.inProgressGrace) }.isInstanceOf(StaleArkistointiExecutionException::class.java)
        assertThatThrownBy { jdbc.update("delete from arkistointi_dynasty_step where job_id=?",e.jobId) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
    }

    @Test
    fun `expired intent is unconfirmed and concurrent attempts admit only one sender`() {
        val e = fixture()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val results = pool.invokeAll(List(2) {
                java.util.concurrent.Callable { ledger.begin(e, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace) }
            }).map { it.get() }
            assertThat(results).containsExactlyInAnyOrder(DynastyStepLedger.Decision.Proceed,DynastyStepLedger.Decision.InProgress)
            val config = properties.getArkistointi().getTurku().dynasty
            val previous = config.inProgressGrace
            try {
                config.inProgressGrace = java.time.Duration.ZERO
                assertThat(ledger.begin(e, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)).isEqualTo(DynastyStepLedger.Decision.Unconfirmed)
            } finally { config.inProgressGrace = previous }
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `database age classifies fresh and expired intents in a non UTC session`() {
        assertThat(jdbc.queryForObject("show TimeZone", String::class.java)).isEqualTo("Pacific/Honolulu")
        val e = fixture()
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) " +
            "values (?,'CASE','ALOITETTU',?,clock_timestamp() - interval '6 minutes')", e.jobId, e.version)
        jdbc.update("insert into arkistointi_dynasty_step(job_id,step_key,status,execution_version,started_at) " +
            "values (?,'TASK','ALOITETTU',?,clock_timestamp())", e.jobId, e.version)
        assertThat(ledger.begin(e, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)).isEqualTo(DynastyStepLedger.Decision.Unconfirmed)
        assertThat(ledger.begin(e, "TASK", properties.getArkistointi().getTurku().dynasty.inProgressGrace)).isEqualTo(DynastyStepLedger.Decision.InProgress)
    }

    @Test
    fun `ledger timestamps store absolute instants generated by the database`() {
        assertThat(jdbc.queryForList(
            "select udt_name from information_schema.columns where table_name='arkistointi_dynasty_step' " +
                "and column_name in ('started_at','completed_at')", String::class.java
        )).containsExactlyInAnyOrder("timestamptz", "timestamptz")
        val e = fixture()
        ledger.begin(e, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)
        assertThat(ledger.complete(e.jobId, "CASE", e.version, "415")).isTrue()
        assertThat(jdbc.queryForObject(
            "select started_at <= completed_at and completed_at <= clock_timestamp() " +
                "and started_at > clock_timestamp() - interval '1 minute' from arkistointi_dynasty_step where job_id=?",
            Boolean::class.javaObjectType, e.jobId
        )).isTrue()
    }

    @Test
    fun `original attempt cannot complete a replacement`() {
        val e = fixture()
        ledger.begin(e, "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)
        ledger.abandon(e.jobId,"CASE",e.version)
        jdbc.update("update scheduled_tasks set version=2 where task_instance=?",e.taskInstanceId)
        ledger.begin(e.copy(version=2), "CASE", properties.getArkistointi().getTurku().dynasty.inProgressGrace)
        assertThat(ledger.complete(e.jobId,"CASE",1,"415")).isFalse()
        assertThat(ledger.complete(e.jobId,"CASE",2,"416")).isTrue()
    }
}
