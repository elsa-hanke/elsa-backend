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
        @Bean fun dataSource() = DriverManagerDataSource("jdbc:tc:postgresql:16.9:///dynastyLedger?TC_DAEMON=true","test","test")
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
    fun `intent is committed and blocks duplicate create after recovery`() {
        val e = fixture()
        assertThat(ledger.begin(e,"CASE")).isEqualTo(DynastyStepLedger.Decision.Proceed)
        jdbc.update("update scheduled_tasks set version=2 where task_instance=?",e.taskInstanceId)
        val recovered = e.copy(version=2)
        assertThat(ledger.begin(recovered,"CASE")).isEqualTo(DynastyStepLedger.Decision.InProgress)
        assertThat(ledger.complete(e.jobId,"CASE",e.version,"415")).isTrue()
        assertThat(ledger.begin(recovered,"CASE")).isEqualTo(DynastyStepLedger.Decision.Done("415"))
        assertThatThrownBy { ledger.begin(e,"TASK") }.isInstanceOf(StaleArkistointiExecutionException::class.java)
        assertThatThrownBy { jdbc.update("delete from arkistointi_dynasty_step where job_id=?",e.jobId) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
    }

    @Test
    fun `expired intent is unconfirmed and concurrent attempts admit only one sender`() {
        val e = fixture()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val results = pool.invokeAll(List(2) {
                java.util.concurrent.Callable { ledger.begin(e,"CASE") }
            }).map { it.get() }
            assertThat(results).containsExactlyInAnyOrder(DynastyStepLedger.Decision.Proceed,DynastyStepLedger.Decision.InProgress)
            val config = properties.getArkistointi().getTurku().dynasty
            val previous = config.inProgressGrace
            try {
                config.inProgressGrace = java.time.Duration.ZERO
                assertThat(ledger.begin(e,"CASE")).isEqualTo(DynastyStepLedger.Decision.Unconfirmed)
            } finally { config.inProgressGrace = previous }
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `original attempt cannot complete a replacement`() {
        val e = fixture()
        ledger.begin(e,"CASE")
        ledger.abandon(e.jobId,"CASE",e.version)
        jdbc.update("update scheduled_tasks set version=2 where task_instance=?",e.taskInstanceId)
        ledger.begin(e.copy(version=2),"CASE")
        assertThat(ledger.complete(e.jobId,"CASE",1,"415")).isFalse()
        assertThat(ledger.complete(e.jobId,"CASE",2,"416")).isTrue()
    }
}
