package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = [
        "spring.datasource.url=jdbc:tc:postgresql:16.9:///arkistointiTransactions?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
    ]
)
class ArkistointiJobImmutabilityIT {
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var scheduler: ArkistointiJobScheduler

    @Test
    fun `complete multi-document manifest commits and allows status and result updates`() {
        val fixture = createFixture(2)
        transactions.executeWithoutResult {
            val job = jobs.findById(fixture.id).orElseThrow()
            job.transitionTo(ArkistointiJobStatus.KASITTELYSSA)
            job.errorCode = "TEMPORARY"
            job.lastError = "Safe description"
            jobs.saveAndFlush(job)
        }
        transactions.executeWithoutResult {
            val job = jobs.findById(fixture.id).orElseThrow()
            job.transitionTo(ArkistointiJobStatus.LAHETETTY)
            job.externalId = "external-id"
            job.completedAt = Instant.now()
            job.errorCode = null
            job.lastError = null
            jobs.saveAndFlush(job)
        }
        val stored = jobs.findById(fixture.id).orElseThrow()
        assertThat(stored.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(stored.externalId).isEqualTo("external-id")
        assertThat(stored.completedAt).isNotNull()
        assertThat(stored.lastError).isNull()
        assertThat(stored.expectedDocumentCount).isEqualTo(2)
        assertThat(referenceCount(fixture.id)).isEqualTo(2)
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', quoteCharacter = '`', value = [
        "id ; gen_random_uuid()",
        "university ; 'HELSINGIN_YLIOPISTO'",
        "case_type ; 'SOPIMUS'",
        "payload ; '{\"private\":\"changed\"}'",
        "job_key ; 'changed-' || id::text",
        "created_at ; created_at - interval '1 day'",
        "expected_document_count ; expected_document_count + 1"
    ])
    fun `SQL cannot change delivery metadata`(column: String, expression: String) {
        val fixture = createFixture()
        rejected {
            jdbc.update("update arkistointi_job set $column = $expression where id = ?", fixture.id)
        }
        assertThat(jobs.findById(fixture.id).orElseThrow().key).isEqualTo(fixture.key)
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', quoteCharacter = '`', value = [
        "id ; id + 1000000",
        "job_id ; ?",
        "asiakirja_id ; ?",
        "asiakirjatyyppi ; 'LIITE'",
        "jarjestys ; jarjestys + 10",
        "tiedostonimi ; 'changed.pdf'",
        "sisaltotyyppi ; 'image/jpeg'",
        "sha256 ; repeat('b', 64)"
    ])
    fun `SQL cannot change any document reference metadata`(column: String, expression: String) {
        val fixture = createFixture()
        val replacement = createFixture()
        rejected {
            val args = when (column) {
                "job_id" -> arrayOf<Any>(replacement.id, fixture.id)
                "asiakirja_id" -> arrayOf<Any>(replacement.documentId, fixture.id)
                else -> arrayOf<Any>(fixture.id)
            }
            jdbc.update("update arkistointi_job_asiakirja set $column = $expression where job_id = ?", *args)
        }
        assertThat(referenceCount(fixture.id)).isEqualTo(1)
    }

    @Test
    fun `consistent replacement of document and checksum is rejected`() {
        val fixture = createFixture()
        val replacement = createFixture()
        rejected {
            jdbc.update("update arkistointi_job_asiakirja set asiakirja_id = ?, sha256 = repeat('b', 64) where job_id = ?",
                replacement.documentId, fixture.id)
        }
    }

    @Test
    fun `JPA cannot change the payload of a persisted job`() {
        val fixture = createFixture()
        rejected {
            jobs.findById(fixture.id).orElseThrow().payload = "{\"changed\":true}"
            em.flush()
        }
        assertThat(jobs.findById(fixture.id).orElseThrow().payload).isEqualTo("{}")
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `database rejects nonpositive expected document counts`(count: Int) {
        rejected {
            jdbc.update("insert into arkistointi_job (university, case_type, payload, job_key, expected_document_count) " +
                "values ('TURUN_YLIOPISTO', 'VALMISTUMINEN', '{}', ?, ?)", "invalid-${UUID.randomUUID()}", count)
        }
    }

    @Test
    fun `no-op updates succeed for job and references`() {
        val fixture = createFixture()
        transactions.executeWithoutResult {
            jdbc.update("update arkistointi_job set id = id, payload = payload, job_key = job_key, university = university, " +
                "case_type = case_type, created_at = created_at, expected_document_count = expected_document_count where id = ?",
                fixture.id)
            jdbc.update("update arkistointi_job_asiakirja set id = id, job_id = job_id, asiakirja_id = asiakirja_id, " +
                "asiakirjatyyppi = asiakirjatyyppi, jarjestys = jarjestys, tiedostonimi = tiedostonimi, " +
                "sisaltotyyppi = sisaltotyyppi, sha256 = sha256 where job_id = ?", fixture.id)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["arkistointi_job", "arkistointi_job_asiakirja"])
    fun `SQL delete and parent cascade cannot discard a delivery`(table: String) {
        val fixture = createFixture()
        val column = if (table == "arkistointi_job") "id" else "job_id"
        rejected { jdbc.update("delete from $table where $column = ?", fixture.id) }
        assertThat(referenceCount(fixture.id)).isEqualTo(1)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `JPA orphan removal and parent deletion are rejected`(parent: Boolean) {
        val fixture = createFixture()
        rejected {
            val job = jobs.findById(fixture.id).orElseThrow()
            if (parent) jobs.delete(job) else job.asiakirjat.clear()
            em.flush()
        }
    }

    @ParameterizedTest
    @CsvSource("1,0", "2,1", "1,2")
    fun `incomplete or excessive manifest rolls back documents job and schedule at commit`(expected: Int, actual: Int) {
        val attempted = AtomicReference<Fixture>()
        rejected { attempted.set(persistFixture(expected, actual)) }
        val fixture = attempted.get()
        assertThat(jobs.existsById(fixture.id)).isFalse()
        assertThat(referenceCount(fixture.id)).isZero()
        assertThat(taskCount(fixture.id)).isZero()
        assertThat(jdbc.queryForObject("select count(*) from asiakirja where id = ?", Long::class.java, fixture.documentId)).isZero()
    }

    @Test
    fun `adding a document to an already committed manifest fails at commit`() {
        val fixture = createFixture()
        rejected { insertReference(fixture) }
        assertThat(referenceCount(fixture.id)).isEqualTo(1)
    }

    @Test
    fun `rollback of complete manifest also removes the schedule`() {
        val fixture = requireNotNull(transactions.execute {
            persistFixture(2, 2).also { _ -> it.setRollbackOnly() }
        })
        assertThat(jobs.existsById(fixture.id)).isFalse()
        assertThat(referenceCount(fixture.id)).isZero()
        assertThat(taskCount(fixture.id)).isZero()
    }

    @Test
    fun `concurrent additions serialize on the parent and neither can change the manifest`() {
        val fixture = createFixture()
        val inserted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val secondPid = AtomicInteger()
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit<String> {
                rejected {
                    insertReference(fixture)
                    inserted.countDown()
                    check(release.await(15, TimeUnit.SECONDS))
                }
            }
            check(inserted.await(15, TimeUnit.SECONDS))
            val second = executor.submit<String> {
                rejected {
                    secondPid.set(jdbc.queryForObject("select pg_backend_pid()", Int::class.java)!!)
                    insertReference(fixture)
                }
            }
            await().atMost(Duration.ofSeconds(10)).untilAsserted {
                assertThat(secondPid.get()).isPositive()
                assertThat(jdbc.queryForObject("select cardinality(pg_blocking_pids(?)) > 0", Boolean::class.java,
                    secondPid.get())).isTrue()
            }
            release.countDown()
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo("23514")
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo("23514")
            assertThat(referenceCount(fixture.id)).isEqualTo(1)
        } finally {
            release.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(15, TimeUnit.SECONDS))
        }
    }

    private fun createFixture(count: Int = 1): Fixture =
        requireNotNull(transactions.execute { persistFixture(count, count) })

    private fun persistFixture(expected: Int, actual: Int): Fixture {
        jdbc.update("insert into yliopisto (nimi) select ? where not exists (select 1 from yliopisto)",
            YliopistoEnum.TURUN_YLIOPISTO.name)
        val document = AsiakirjaHelper.createEntity(em)
        em.persist(document)
        val job = ArkistointiJob(
            university = YliopistoEnum.TURUN_YLIOPISTO, caseType = CaseType.VALMISTUMINEN,
            payload = "{}", key = "manifest-${UUID.randomUUID()}", expectedDocumentCount = expected
        )
        repeat(actual) { order ->
            job.lisaaAsiakirja(ArkistointiJobAsiakirja(
                asiakirja = document, asiakirjatyyppi = RecordType.YHTEENVETO, jarjestysnumero = order,
                filename = "document.pdf", contentType = "application/pdf", sha256 = "a".repeat(64)
            ))
        }
        jobs.saveAndFlush(job)
        val id = requireNotNull(job.id)
        check(scheduler.schedule(id, Instant.parse("2099-01-01T00:00:00Z")))
        return Fixture(id, requireNotNull(document.id), requireNotNull(job.key))
    }

    private fun insertReference(fixture: Fixture) {
        jdbc.update("insert into arkistointi_job_asiakirja " +
            "(job_id, asiakirja_id, asiakirjatyyppi, jarjestys, tiedostonimi, sisaltotyyppi, sha256) " +
            "values (?, ?, 'LIITE', 99, 'extra.pdf', 'application/pdf', repeat('b', 64))",
            fixture.id, fixture.documentId)
    }

    private fun rejected(action: () -> Unit): String {
        val exception = assertThrows<RuntimeException> { transactions.executeWithoutResult { action() } }
        val sqlException = generateSequence<Throwable>(exception) { it.cause }.filterIsInstance<SQLException>().first()
        assertThat(sqlException.sqlState).isEqualTo("23514")
        return sqlException.sqlState
    }

    private fun referenceCount(id: UUID) = jdbc.queryForObject(
        "select count(*) from arkistointi_job_asiakirja where job_id = ?", Long::class.java, id)

    private fun taskCount(id: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Long::class.java, id.toString())

    private data class Fixture(val id: UUID, val documentId: Long, val key: String)
}
