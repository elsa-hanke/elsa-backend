package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.kayttaja.AsiakirjaRepository
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest(
    classes = [ElsaBackendApp::class],
    properties = [
        "spring.datasource.url=jdbc:tc:postgresql:16.9:///arkistointiTransactions?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
    ]
)
class ArkistointiJobManifestIT {
    @Autowired private lateinit var creator: ArkistointiJobCreator
    @Autowired private lateinit var loader: ArkistointiAsiakirjaLoader
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var documents: AsiakirjaRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var transactions: TransactionTemplate

    @Test
    fun `creator commits a complete immutable manifest and schedule with original bytes`() {
        val sources = listOf(source(1), source(2))
        val id = createJob(sources)
        val manifest = readManifest(id)

        assertThat(manifest.expectedCount).isEqualTo(2)
        assertThat(manifest.documents.map { it.order }).containsExactly(0, 1)
        manifest.documents.zip(sources).forEach { (snapshot, source) ->
            assertThat(snapshot.documentId).isNotEqualTo(source.id)
            assertThat(snapshot.bytes).containsExactly(*source.bytes)
            assertThat(snapshot.checksum).isEqualTo(ArkistointiChecksum.sha256(source.bytes))
        }
        assertThat(taskCount(id)).isEqualTo(1)
        assertManifestUnchanged(manifest)
    }

    @ParameterizedTest
    @ValueSource(strings = ["reference", "contents", "snapshot-metadata", "append", "delete-reference", "delete-snapshot", "payload"])
    fun `database protections keep snapshots and delivery manifest consistent`(change: String) {
        val original = readManifest(createJob(listOf(source(1))))
        val replacement = readManifest(createJob(listOf(source(2)))).documents.single()
        val snapshot = original.documents.single()
        val exception = assertThrows<RuntimeException> {
            transactions.executeWithoutResult {
                when (change) {
                    "reference" -> jdbc.update(
                        "update arkistointi_job_asiakirja set asiakirja_id = ?, sha256 = ? where job_id = ?",
                        replacement.documentId, replacement.checksum, original.id
                    )
                    "contents" -> jdbc.update("update asiakirja_data set data = ? where id = ?", replacement.bytes, snapshot.dataId)
                    "snapshot-metadata" -> jdbc.update("update asiakirja set nimi = 'changed.pdf' where id = ?", snapshot.documentId)
                    "append" -> insertExtraReference(original.id, replacement)
                    "delete-reference" -> jdbc.update("delete from arkistointi_job_asiakirja where job_id = ?", original.id)
                    "delete-snapshot" -> jdbc.update("delete from asiakirja where id = ?", snapshot.documentId)
                    else -> jdbc.update("update arkistointi_job set payload = '{}' where id = ?", original.id)
                }
            }
        }
        assertConstraintFailure(exception)
        assertManifestUnchanged(original)
    }

    @Test
    fun `manifest failure at commit rolls back newly created snapshots job references and schedule`() {
        val source = source(1)
        val documentsBefore = documents.count()
        val jobsBefore = jobs.count()
        val tasksBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.java)
        val attemptedId = AtomicReference<UUID>()
        val callbackFinished = AtomicBoolean()

        val exception = assertThrows<RuntimeException> {
            transactions.executeWithoutResult {
                val job = creator.create(request(listOf(source)))
                attemptedId.set(requireNotNull(job.id))
                assertThat(documents.count()).isEqualTo(documentsBefore + 1)
                assertThat(taskCount(attemptedId.get())).isEqualTo(1)
                insertExtraReference(attemptedId.get(), readManifest(attemptedId.get()).documents.single())
                em.flush()
                callbackFinished.set(true)
            }
        }

        assertThat(callbackFinished.get()).isTrue()
        assertConstraintFailure(exception)
        assertThat(documents.count()).isEqualTo(documentsBefore)
        assertThat(jobs.count()).isEqualTo(jobsBefore)
        assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.java)).isEqualTo(tasksBefore)
        assertThat(jobs.existsById(attemptedId.get())).isFalse()
        assertThat(jdbc.queryForObject("select count(*) from arkistointi_job_asiakirja where job_id = ?",
            Long::class.java, attemptedId.get())).isZero()
        assertThat(documents.findOneWithDataById(source.id)!!.asiakirjaData!!.data).containsExactly(*source.bytes)
    }

    @Test
    fun `repeated creation keeps the original UUID payload references and manifest size`() {
        val sources = listOf(source(1), source(2))
        val key = "manifest-repeat-${UUID.randomUUID()}"
        val original = readManifest(createJob(sources, key))
        val changedSource = source(3)
        val documentsBefore = documents.count()
        val jobsBefore = jobs.count()
        val tasksBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.java)

        val repeatedId = transactions.execute {
            creator.create(request(listOf(changedSource), key).copy(payload = "{\"changed\":true}")).id
        }

        assertThat(repeatedId).isEqualTo(original.id)
        assertThat(documents.count()).isEqualTo(documentsBefore)
        assertThat(jobs.count()).isEqualTo(jobsBefore)
        assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.java)).isEqualTo(tasksBefore)
        assertManifestUnchanged(original)
    }

    @Test
    fun `loader still rejects a wrong checksum even when persisted references are immutable`() {
        val original = readManifest(createJob(listOf(source(1))))
        val exception = assertThrows<ArkistointiAsiakirjaException> {
            transactions.executeWithoutResult {
                val reference = jobs.findById(original.id).orElseThrow().asiakirjat.single()
                loader.load(reference.toDescriptor().copy(sha256 = "a".repeat(64)))
            }
        }
        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
        assertManifestUnchanged(original)
    }

    private fun source(pages: Int): Source = requireNotNull(transactions.execute {
        jdbc.update("insert into yliopisto (nimi) select ? where not exists (select 1 from yliopisto)",
            YliopistoEnum.TURUN_YLIOPISTO.name)
        val document = AsiakirjaHelper.createEntity(em)
        val bytes = PdfTestData.unembeddedFontPdf(pages)
        document.asiakirjaData!!.data = bytes.copyOf()
        em.persist(document)
        em.flush()
        Source(requireNotNull(document.id), bytes)
    })

    private fun request(sources: List<Source>, key: String = "manifest-${UUID.randomUUID()}") = CreateArkistointiJobRequest(
        YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{\"approved\":true}", key,
        sources.mapIndexed { order, source ->
            val document = documents.findOneWithDataById(source.id)!!
            ArkistointiAsiakirjaReference(document, RecordType.LIITE, order, document.nimi!!, document.tyyppi!!,
                ArkistointiChecksum.sha256(source.bytes))
        }
    )

    private fun createJob(sources: List<Source>, key: String = "manifest-${UUID.randomUUID()}"): UUID =
        requireNotNull(transactions.execute { creator.create(request(sources, key)).id })

    private fun readManifest(id: UUID): Manifest = requireNotNull(transactions.execute {
        val job = jobs.findById(id).orElseThrow()
        Manifest(id, requireNotNull(job.payload), requireNotNull(job.expectedDocumentCount), job.asiakirjat.map { reference ->
            assertThat(reference.asiakirja!!.archiveSnapshot).isTrue()
            Snapshot(requireNotNull(reference.asiakirja!!.id), requireNotNull(reference.asiakirja!!.asiakirjaData!!.id),
                requireNotNull(reference.sha256), loader.load(reference.toDescriptor()).sisalto, requireNotNull(reference.filename),
                requireNotNull(reference.contentType), requireNotNull(reference.jarjestysnumero))
        })
    })

    private fun assertManifestUnchanged(original: Manifest) {
        val current = readManifest(original.id)
        assertThat(current.payload).isEqualTo(original.payload)
        assertThat(current.expectedCount).isEqualTo(original.expectedCount)
        assertThat(current.documents).hasSameSizeAs(original.documents)
        current.documents.zip(original.documents).forEach { (actual, expected) ->
            assertThat(actual.documentId).isEqualTo(expected.documentId)
            assertThat(actual.dataId).isEqualTo(expected.dataId)
            assertThat(actual.checksum).isEqualTo(expected.checksum)
            assertThat(actual.filename).isEqualTo(expected.filename)
            assertThat(actual.contentType).isEqualTo(expected.contentType)
            assertThat(actual.order).isEqualTo(expected.order)
            assertThat(actual.bytes).containsExactly(*expected.bytes)
        }
        assertThat(taskCount(original.id)).isEqualTo(1)
    }

    private fun insertExtraReference(id: UUID, document: Snapshot) {
        jdbc.update("insert into arkistointi_job_asiakirja " +
            "(job_id, asiakirja_id, asiakirjatyyppi, jarjestys, tiedostonimi, sisaltotyyppi, sha256) " +
            "values (?, ?, 'LIITE', 99, ?, ?, ?)",
            id, document.documentId, document.filename, document.contentType, document.checksum)
    }

    private fun assertConstraintFailure(exception: RuntimeException) {
        val sqlException = generateSequence<Throwable>(exception) { it.cause }.filterIsInstance<SQLException>().first()
        assertThat(sqlException.sqlState).isEqualTo("23514")
    }

    private fun taskCount(id: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Long::class.java, id.toString())

    private data class Source(val id: Long, val bytes: ByteArray)
    private data class Snapshot(
        val documentId: Long, val dataId: Long, val checksum: String, val bytes: ByteArray,
        val filename: String, val contentType: String, val order: Int
    )
    private data class Manifest(val id: UUID, val payload: String, val expectedCount: Int, val documents: List<Snapshot>)
}
