package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ElsaBackendApp::class])
class ArkistointiJobCreatorIT {

    @Autowired
    private lateinit var creator: ArkistointiJobCreator

    @Autowired
    private lateinit var snapshotService: ArkistointiAsiakirjaSnapshotService

    @Autowired
    private lateinit var jobRepository: ArkistointiJobRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @Transactional
    fun `snapshot references and scheduler row are stored in the approval transaction`() {
        val source = AsiakirjaHelper.createEntity(entityManager)
        entityManager.persist(source)
        val reference = snapshotService.createSnapshot(
            opintooikeus = source.opintooikeus!!,
            filename = "approval-document.pdf",
            contentType = MediaType.APPLICATION_PDF_VALUE,
            data = source.asiakirjaData?.data,
            asiakirjatyyppi = RecordType.YHTEENVETO,
            jarjestysnumero = 0
        )

        val job = creator.create(
            CreateArkistointiJobRequest(
                university = YliopistoEnum.TURUN_YLIOPISTO,
                caseType = CaseType.VALMISTUMINEN,
                payload = "{\"caseId\":123}",
                key = "valmistuminen-pdfa-it-${source.id}",
                asiakirjat = listOf(reference)
            )
        )

        entityManager.flush()
        entityManager.clear()
        val stored = jobRepository.findById(job.id!!).orElseThrow()
        assertThat(stored.asiakirjat).hasSize(1)
        assertThat(stored.asiakirjat.single().asiakirja?.id)
            .isEqualTo(reference.asiakirja.id)
        assertThat(stored.asiakirjat.single().sha256).isEqualTo(reference.sha256)
        assertThat(scheduledTaskCount(job.id!!)).isEqualTo(1)
    }

    private fun scheduledTaskCount(jobId: Long): Long =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from scheduled_tasks
            where task_name = ? and task_instance = ?
            """.trimIndent(),
            Long::class.javaObjectType,
            ARKISTOINTI_JOB_TASK.taskName,
            jobId.toString()
        ) ?: 0
}
