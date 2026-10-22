package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = [
        // Commit tests have their own temporary database so their domain fixtures
        // cannot affect other integration tests sharing the default test database.
        "spring.datasource.url=jdbc:tc:postgresql:16.9:///arkistointiTransactions?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
    ]
)
class DbSchedulerArkistointiJobTransactionIT {

    @Autowired
    private lateinit var jobRepository: ArkistointiJobRepository

    @Autowired
    private lateinit var scheduler: ArkistointiJobScheduler

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `job asiakirjaviite ja ajastus commitoituvat tai peruuntuvat yhdessa`(rollback: Boolean) {
        val jobId = requireNotNull(transactionTemplate.execute { transaction ->
            // University reference data is loaded through SQL in deployed databases.
            jdbcTemplate.update(
                "insert into yliopisto (nimi) select ? where not exists (select 1 from yliopisto)",
                YliopistoEnum.TURUN_YLIOPISTO.name
            )
            val asiakirja = AsiakirjaHelper.createEntity(entityManager)
            entityManager.persist(asiakirja)
            val job = ArkistointiJob(
                university = YliopistoEnum.TURUN_YLIOPISTO,
                caseType = CaseType.VALMISTUMINEN,
                payload = "{\"caseId\":123}",
                expectedDocumentCount = 1,
                key = "transaction-${UUID.randomUUID()}"
            )
            job.lisaaAsiakirja(
                ArkistointiJobAsiakirja(
                    asiakirja = asiakirja,
                    asiakirjatyyppi = RecordType.YHTEENVETO,
                    jarjestysnumero = 0,
                    filename = "yhteenveto.pdf",
                    contentType = "application/pdf",
                    sha256 = "a".repeat(64)
                )
            )
            val saved = jobRepository.saveAndFlush(job)
            val id = requireNotNull(saved.id)
            assertThat(scheduler.schedule(id, Instant.parse("2099-01-01T00:00:00Z"))).isTrue
            if (rollback) {
                transaction.setRollbackOnly()
            }
            id
        })

        val expectedCount = if (rollback) 0L else 1L
        assertThat(jobRepository.existsById(jobId)).isEqualTo(!rollback)
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from arkistointi_job_asiakirja where job_id = ?",
                Long::class.javaObjectType,
                jobId
            )
        ).isEqualTo(expectedCount)
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from scheduled_tasks where task_name = ? and task_instance = ?",
                Long::class.javaObjectType,
                ARKISTOINTI_JOB_TASK.taskName,
                jobId.toString()
            )
        ).isEqualTo(expectedCount)
    }
}
