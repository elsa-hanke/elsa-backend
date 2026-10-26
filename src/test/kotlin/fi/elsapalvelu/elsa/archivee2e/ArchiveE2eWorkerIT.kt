package fi.elsapalvelu.elsa.archivee2e

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import java.time.Duration
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

/** Exercise fixture discovery and the real worker without running the browser or an external archive. */
@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = [
        "spring.datasource.url=jdbc:tc:postgresql:16.9:///archiveE2eWorker?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false"
    ]
)
@ActiveProfiles("test", "archive-e2e")
class ArchiveE2eWorkerIT {
    @Autowired
    private lateinit var creator: ArkistointiJobCreator

    @Autowired
    private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var transactions: TransactionTemplate

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun `committed snapshot job is picked up by real worker and reaches the test adapter`() {
        val id = requireNotNull(transactions.execute {
            // Universities are provisioned as reference SQL, not audited JPA inserts.
            jdbc.update("insert into yliopisto (nimi) select ? where not exists (select 1 from yliopisto)",
                YliopistoEnum.HELSINGIN_YLIOPISTO.name)
            val source = AsiakirjaHelper.createEntity(em).also(em::persist)
            creator.create(CreateArkistointiJobRequest(
                YliopistoEnum.HELSINGIN_YLIOPISTO, CaseType.SOPIMUS,
                """{"caseId":42,"yek":false,"approver":"E2E Approver"}""",
                "archive-e2e-worker:${UUID.randomUUID()}",
                listOf(snapshots.createReference(source, RecordType.SOPIMUS, 0))
            )).id
        })
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            val job = jdbc.queryForMap("select status, external_id, completed_at from arkistointi_job where id = ?", id)
            assertThat(job["status"]).isEqualTo("LAHETETTY")
            assertThat(job["external_id"]).isEqualTo("e2e:$id")
            assertThat(job["completed_at"]).isNotNull()
            val receipt = jdbc.queryForMap("select delivery_key, invocation_count from archive_e2e.receipt where job_id = ?", id)
            assertThat(receipt["delivery_key"]).isEqualTo(id)
            assertThat(receipt["invocation_count"]).isEqualTo(1)
            assertThat(jdbc.queryForObject(
                "select count(*) from scheduled_tasks where task_name = 'arkistointi-job' and task_instance = ?",
                Long::class.javaObjectType, id.toString()
            )).isZero()
        }
    }
}
