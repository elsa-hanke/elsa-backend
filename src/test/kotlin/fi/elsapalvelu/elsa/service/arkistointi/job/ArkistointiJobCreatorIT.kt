package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.ArchiveImageTestData
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

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

    @Autowired
    private lateinit var transactions: TransactionTemplate

    @ParameterizedTest
    @ValueSource(strings = ["pdf", "image"])
    @Transactional
    fun `untrusted mutable source produces a snapshot with its own checksum and metadata`(kind: String) {
        val source = AsiakirjaHelper.createEntity(entityManager)
        source.asiakirjaData!!.data = if (kind == "pdf") PdfTestData.unembeddedFontPdf(1) else ArchiveImageTestData.image()
        source.tyyppi = if (kind == "pdf") "application/pdf" else "image/jpeg"
        source.nimi = if (kind == "pdf") "source.pdf" else "source.jpg"
        entityManager.persist(source)
        entityManager.flush()
        // The database flag remains false: this in-memory claim cannot establish immutability.
        source.archiveSnapshot = true
        val input = source.asiakirjaData!!.data!!.copyOf()
        val job = creator.create(request(ArkistointiAsiakirjaReference(source, RecordType.LIITE, 0,
            source.nimi!!, source.tyyppi!!, ArkistointiChecksum.sha256(input))))
        entityManager.flush()
        entityManager.clear()
        val attached = jobRepository.findById(job.id!!).orElseThrow().asiakirjat.single()
        val bytes = loader.load(attached).sisalto
        assertThat(attached.asiakirja!!.id).isNotEqualTo(source.id)
        assertThat(attached.filename).isEqualTo("source.pdf")
        assertThat(attached.contentType).isEqualTo("application/pdf")
        assertThat(attached.sha256).isEqualTo(ArkistointiChecksum.sha256(bytes)).isNotEqualTo(ArkistointiChecksum.sha256(input))
        assertThat(pdfA2bService.isPdfA2b(bytes)).isTrue()
        assertThat(entityManager.find(fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja::class.java, source.id).archiveSnapshot).isFalse()
        assertThat(scheduledTaskCount(job.id!!)).isEqualTo(1)
    }

    @Test
    @Transactional
    fun `persisted snapshot flag cannot certify a nonconforming PDF`() {
        val source = AsiakirjaHelper.createEntity(entityManager)
        source.archiveSnapshot = true
        source.asiakirjaData!!.data = PdfTestData.unembeddedFontPdf(1)
        entityManager.persist(source)
        val reference = ArkistointiAsiakirjaReference(source, RecordType.LIITE, 0,
            source.nimi!!, source.tyyppi!!, ArkistointiChecksum.sha256(source.asiakirjaData!!.data!!))
        val exception = assertThrows<ArkistointiAsiakirjaException> { creator.create(request(reference)) }
        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_NOT_PDFA_2B")
        assertThat(exception.category).isEqualTo(ArkistointiAsiakirjaErrorCategory.STORED_DOCUMENT)
    }

    @ParameterizedTest
    @ValueSource(strings = ["input", "checksum"])
    fun `validation failure rolls back new snapshots job and schedule`(kind: String) {
        val documentsBefore = asiakirjaRepository.count()
        val jobsBefore = jobRepository.count()
        val tasksBefore = jdbcTemplate.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)
        val exception = assertThrows<ArkistointiAsiakirjaException> {
            transactions.executeWithoutResult {
                val first = storedSnapshot()
                if (kind == "input") {
                    creator.create(request(first))
                    snapshotService.createSnapshot(requireNotNull(first.asiakirja.opintooikeus),
                        "private-patient.pdf", "application/pdf", null, RecordType.LIITE, 1)
                } else {
                    val second = storedSnapshot().copy(jarjestysnumero = 1, sha256 = "a".repeat(64))
                    creator.create(request(first).copy(asiakirjat = listOf(first, second)))
                }
            }
        }
        assertThat(exception.category).isEqualTo(if (kind == "input")
            ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT else ArkistointiAsiakirjaErrorCategory.STORED_DOCUMENT)
        assertThat(exception.cause).isNull()
        assertThat(exception.stackTraceToString()).doesNotContain("private-patient.pdf")
        assertThat(asiakirjaRepository.count()).isEqualTo(documentsBefore)
        assertThat(jobRepository.count()).isEqualTo(jobsBefore)
        assertThat(jdbcTemplate.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)).isEqualTo(tasksBefore)
    }

    private fun request(reference: ArkistointiAsiakirjaReference) = CreateArkistointiJobRequest(
        YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{}", "snapshot-validation-${reference.asiakirja.id}", listOf(reference)
    )

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

    @ParameterizedTest
    @ValueSource(strings = ["image/jpg", "image/png"])
    @Transactional
    fun `large certificate image is archived without modifying its stored original`(contentType: String) {
        val opintooikeus = AsiakirjaHelper.createEntity(entityManager).opintooikeus!!
        val student = opintooikeus.erikoistuvaLaakari!!
        val original = ArchiveImageTestData.image(if (contentType == "image/jpg") "jpeg" else "png", 4000, 3000)
        val filename = "certificate.${contentType.substringAfter('/')}"
        student.laillistamistodistus!!.data = original.copyOf()
        student.laillistamispaivanLiitetiedostonNimi = filename
        student.laillistamispaivanLiitetiedostonTyyppi = contentType
        entityManager.flush()
        val sourceId = student.laillistamistodistus!!.id

        val reference = snapshotService.createLaillistamistodistusReference(opintooikeus, 0)
        val job = creator.create(
            CreateArkistointiJobRequest(
                YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{\"approved\":true}",
                "certificate-image-${opintooikeus.id}", listOf(reference)
            )
        )
        entityManager.flush()
        entityManager.clear()

        val storedStudent = entityManager.find(ErikoistuvaLaakari::class.java, student.id)
        assertThat(storedStudent.laillistamistodistus!!.id).isEqualTo(sourceId)
        assertThat(storedStudent.laillistamistodistus!!.data).containsExactly(*original)
        assertThat(storedStudent.laillistamispaivanLiitetiedostonNimi).isEqualTo(filename)
        assertThat(storedStudent.laillistamispaivanLiitetiedostonTyyppi).isEqualTo(contentType)
        val storedJob = jobRepository.findById(job.id!!).orElseThrow()
        val storedReference = storedJob.asiakirjat.single()
        val snapshotBytes = loader.load(storedReference).sisalto
        assertThat(pdfA2bService.isPdfA2b(snapshotBytes)).isTrue
        assertThat(storedReference.sha256).isEqualTo(ArkistointiChecksum.sha256(snapshotBytes))
        assertThat(storedReference.filename).isEqualTo("certificate.pdf")
        assertThat(storedReference.contentType).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        assertThat(storedReference.asiakirja!!.archiveSnapshot).isTrue
        assertThat(storedReference.asiakirja!!.asiakirjaData!!.id).isNotEqualTo(sourceId)
        assertThat(scheduledTaskCount(job.id!!)).isEqualTo(1)
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
        val submission = ArkistointiSubmission(
            requireNotNull(stored.id), requireNotNull(stored.university), requireNotNull(stored.caseType),
            requireNotNull(stored.payload), stored.asiakirjat.map(loader::load)
        )
        assertThat(submission.key).isEqualTo(job.id)
        assertThat(submission.key.toString()).isNotEqualTo(stored.key)
        assertThat(jdbcTemplate.queryForObject(
            "select task_instance from scheduled_tasks where task_name = ? and task_instance = ?",
            String::class.java, ARKISTOINTI_JOB_TASK.taskName, submission.key.toString()
        )).isEqualTo(submission.key.toString())
    }

    private fun scheduledTaskCount(jobId: UUID): Long =
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
