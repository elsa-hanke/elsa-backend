package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ArkistointiJobRepositoryIT {

    @Autowired
    private lateinit var jobRepository: ArkistointiJobRepository

    @Autowired
    private lateinit var asiakirjaRepository: ArkistointiJobAsiakirjaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `job ja asiakirjaviite tallennetaan ja haetaan`() {
        val asiakirja = AsiakirjaHelper.createEntity(entityManager)
        entityManager.persist(asiakirja)
        val job = uusiJob("tallennus-1")
        job.createdAt = null
        job.updatedAt = null
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

        val tallennettu = jobRepository.saveAndFlush(job)
        entityManager.clear()

        val loydetty = jobRepository.findByKey("tallennus-1")
        val asiakirjat = asiakirjaRepository
            .findAllByJobIdOrderByJarjestysnumeroAsc(tallennettu.id!!)

        assertThat(loydetty).isNotNull
        assertThat(loydetty?.createdAt).isNotNull
        assertThat(loydetty?.updatedAt).isNotNull
        assertThat(loydetty?.id).isEqualTo(tallennettu.id)
        assertThat(asiakirjat).hasSize(1)
        assertThat(asiakirjat.single().asiakirja?.id).isEqualTo(asiakirja.id)
        assertThat(asiakirjat.single().job?.id).isEqualTo(tallennettu.id)
    }

    @Test
    fun `samaa key arvoa ei voi tallentaa kahdesti`() {
        jobRepository.saveAndFlush(uusiJob("sama-avain"))

        assertThrows<DataIntegrityViolationException> {
            jobRepository.saveAndFlush(uusiJob("sama-avain"))
        }
    }

    @Test
    fun `different business operations cannot share a delivery UUID`() {
        val first = jobRepository.saveAndFlush(uusiJob("first-delivery"))

        assertThrows<DataIntegrityViolationException> {
            jdbcTemplate.update(
                "insert into arkistointi_job (id, university, case_type, payload, job_key, expected_document_count) " +
                    "values (?, ?, ?, ?, ?, 1)",
                first.id, "TURUN_YLIOPISTO", "VALMISTUMINEN", "{}", "different-business-key"
            )
        }
    }

    @Test
    fun `JPA generates independent UUID job IDs when first persisted`() {
        val first = uusiJob("generated-first")
        val second = uusiJob("generated-second")
        assertThat(first.id).isNull()
        assertThat(second.id).isNull()

        jobRepository.saveAndFlush(first)
        jobRepository.saveAndFlush(second)

        val firstId = requireNotNull(first.id)
        assertThat(firstId.version()).isEqualTo(4)
        assertThat(firstId.variant()).isEqualTo(2)
        assertThat(second.id).isNotNull().isNotEqualTo(firstId)
    }

    @Test
    fun `persisted delivery UUID survives reloads automatic retries and manual retry transitions`() {
        val saved = jobRepository.saveAndFlush(uusiJob("retry-delivery"))
        val id = requireNotNull(saved.id)
        entityManager.clear()

        listOf(
            ArkistointiJobStatus.KASITTELYSSA,
            ArkistointiJobStatus.ODOTTAA,
            ArkistointiJobStatus.KASITTELYSSA,
            ArkistointiJobStatus.VAATII_TOIMENPITEITA,
            ArkistointiJobStatus.ODOTTAA,
            ArkistointiJobStatus.KASITTELYSSA,
            ArkistointiJobStatus.LAHETETTY
        ).forEach { status ->
            val current = jobRepository.findById(id).orElseThrow()
            assertThat(current.id).isEqualTo(id)
            current.transitionTo(status)
            jobRepository.saveAndFlush(current)
            entityManager.clear()
        }
        assertThat(jobRepository.findByKey("retry-delivery")?.id).isEqualTo(id)
    }

    @Test
    fun `database rejects changing the delivery UUID`() {
        val job = jobRepository.saveAndFlush(uusiJob("immutable-delivery"))

        val exception = assertThrows<DataIntegrityViolationException> {
            jdbcTemplate.update("update arkistointi_job set id = ? where id = ?", UUID.randomUUID(), job.id)
        }
        assertThat(exception).hasStackTraceContaining("Archive job ID cannot be changed")
    }

    @Test
    fun `database generates distinct delivery UUIDs for inserts without an explicit key`() {
        val sql = "insert into arkistointi_job (university, case_type, payload, job_key, expected_document_count) " +
            "values (?, ?, ?, ?, 1) returning id"
        val first = jdbcTemplate.queryForObject(sql, UUID::class.java, "TURUN_YLIOPISTO", "VALMISTUMINEN", "{}", "sql-first")
        val second = jdbcTemplate.queryForObject(sql, UUID::class.java, "TURUN_YLIOPISTO", "VALMISTUMINEN", "{}", "sql-second")

        assertThat(first).isNotNull()
        assertThat(second).isNotNull().isNotEqualTo(first)
        assertThat(jobRepository.findByKey("sql-first")?.id).isEqualTo(first)
    }

    @Test
    fun `database rejects a missing delivery UUID`() {
        assertThrows<DataIntegrityViolationException> {
            jdbcTemplate.update(
                "insert into arkistointi_job (university, case_type, payload, job_key, id, expected_document_count) " +
                    "values (?, ?, ?, ?, null, 1)",
                "TURUN_YLIOPISTO", "VALMISTUMINEN", "{}", "null-delivery"
            )
        }
    }

    @Test
    fun `updated at paivittyy jobia muutettaessa`() {
        val job = jobRepository.saveAndFlush(uusiJob("paivitysaika"))
        job.updatedAt = Instant.EPOCH
        job.status = ArkistointiJobStatus.KASITTELYSSA

        jobRepository.saveAndFlush(job)

        assertThat(job.updatedAt).isAfter(Instant.EPOCH)
    }

    private fun uusiJob(key: String) = ArkistointiJob(
        university = YliopistoEnum.TURUN_YLIOPISTO,
        caseType = CaseType.VALMISTUMINEN,
        payload = "{\"asia\":\"valmistuminen\"}",
        expectedDocumentCount = 1,
        key = key
    )
}
