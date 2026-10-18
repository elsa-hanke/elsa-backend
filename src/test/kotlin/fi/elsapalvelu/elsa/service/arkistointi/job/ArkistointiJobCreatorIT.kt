package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.server.ResponseStatusException

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

    @Autowired
    private lateinit var asiakirjaRepository: AsiakirjaRepository

    @Autowired
    private lateinit var asiakirjaService: AsiakirjaService

    @Autowired
    private lateinit var pdfA2bService: PdfA2bService

    @Autowired
    private lateinit var loader: ArkistointiAsiakirjaLoader

    @Test
    @Transactional
    fun `changing the source does not change archived bytes or metadata`() {
        val source = AsiakirjaHelper.createEntity(entityManager)
        source.asiakirjaData!!.data = pdfA2bService.normalize(source.asiakirjaData!!.data, source.tyyppi)
        entityManager.persist(source)
        val original = source.asiakirjaData!!.data!!.copyOf()
        // Even a caller constructing references directly cannot attach a mutable source to a job.
        val reference = ArkistointiAsiakirjaReference(
            source, RecordType.LIITE, 0, source.nimi!!, source.tyyppi!!, ArkistointiChecksum.sha256(original)
        )
        val job = creator.create(
            CreateArkistointiJobRequest(
                YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{\"approved\":true}",
                "immutable-source-${source.id}", listOf(reference)
            )
        )
        val snapshotId = job.asiakirjat.single().asiakirja!!.id!!
        assertThat(snapshotId).isNotEqualTo(source.id)
        source.asiakirjaData!!.data = "replacement".toByteArray()
        source.nimi = "replacement.pdf"
        entityManager.flush()
        entityManager.clear()
        val stored = jobRepository.findById(job.id!!).orElseThrow()
        assertThat(loader.load(stored.asiakirjat.single()).sisalto).containsExactly(*original)
        assertThat(stored.payload).isEqualTo("{\"approved\":true}")
        assertThat(stored.asiakirjat.single().filename).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI)
        assertThat(asiakirjaRepository.findAllByOpintooikeusId(source.opintooikeus!!.id!!).map { it.id })
            .doesNotContain(snapshotId)
        assertThat(asiakirjaRepository.findAllByOpintooikeusIdAndTyoskentelyjaksoId(source.opintooikeus!!.id!!, null).map { it.id })
            .doesNotContain(snapshotId)
        val exception = assertThrows<ResponseStatusException> {
            asiakirjaService.delete(snapshotId, source.opintooikeus!!.id!!)
        }
        assertThat(exception.statusCode.value()).isEqualTo(409)
    }

    @Test
    @Transactional
    fun `database prevents changing archived contents`() {
        val reference = storedSnapshot()
        entityManager.flush()
        val dataId = reference.asiakirja.asiakirjaData!!.id!!
        assertThrows<DataIntegrityViolationException> {
            jdbcTemplate.update("update asiakirja_data set data = ? where id = ?", "changed".toByteArray(), dataId)
        }
    }

    @Test
    @Transactional
    fun `database prevents deleting an unqueued snapshot`() {
        val reference = storedSnapshot()
        entityManager.flush()
        assertThrows<DataIntegrityViolationException> {
            jdbcTemplate.update("delete from asiakirja where id = ?", reference.asiakirja.id!!)
        }
    }

    @Test
    @Transactional
    fun `database prevents changing snapshot metadata or unmarking a snapshot`() {
        val reference = storedSnapshot()
        entityManager.flush()
        assertThrows<DataIntegrityViolationException> {
            jdbcTemplate.update("update asiakirja set archive_snapshot = false, nimi = 'changed.pdf' where id = ?", reference.asiakirja.id!!)
        }
    }

    private fun storedSnapshot(): ArkistointiAsiakirjaReference {
        val source = AsiakirjaHelper.createEntity(entityManager)
        entityManager.persist(source)
        return snapshotService.createSnapshot(
            source.opintooikeus!!, source.nimi, source.tyyppi, source.asiakirjaData!!.data, RecordType.LIITE, 0
        )
    }

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
