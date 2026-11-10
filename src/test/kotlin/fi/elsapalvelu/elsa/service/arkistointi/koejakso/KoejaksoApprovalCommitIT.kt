package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessor
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiProcessingException
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobEnqueuer
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.IllegalTransactionStateException

/** Successful approvals: what is committed, idempotency, resubmission and delivery of the committed job. */
class KoejaksoApprovalCommitIT : KoejaksoArchiveItBase() {
    @Autowired private lateinit var enqueuer: ArkistointiJobEnqueuer
    @Autowired private lateinit var archiveService: KoejaksoArkistointiService
    @Autowired private lateinit var processor: ArkistointiJobProcessor

    @Test
    fun `enqueue requires the approval transaction before evaluating document creation`() {
        assertThrows<IllegalTransactionStateException> {
            enqueuer.enqueue(YliopistoEnum.TAMPEREEN_YLIOPISTO, CaseType.KOEJAKSO, 1,
                { error("Documents must not be generated without a transaction") }) {
                error("Payload must not be created without a transaction")
            }
        }
    }

    @ForEachKoejaksoCaseType
    fun `approval commits one immutable PDF and a scheduled job with approval time metadata`(type: CaseType) {
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        val job = job(fixture, type)
        assertThat(job.caseType).isEqualTo(type)
        assertThat(job.expectedDocumentCount).isEqualTo(1)
        assertThat(taskCount(job.id!!)).isEqualTo(1)
        val payload = ArchivePayloadReader(objectMapper).read(job.payload!!)
        assertThat(payload.approvalDate).isEqualTo(LocalDate.now())
        assertThat(payload.caseId).isEqualTo(fixture.caseId(type))
        assertThat(payload.studyRightId).isEqualTo(fixture.studyRightId)
        transactions.executeWithoutResult {
            val reference = jobs.findById(job.id!!).orElseThrow().asiakirjat.single()
            assertThat(reference.asiakirjatyyppi).isEqualTo(if (type == CaseType.SOPIMUS) RecordType.SOPIMUS else RecordType.ARVIOINTI)
            val snapshot = reference.asiakirja!!
            val original = asiakirjaRepository.findAllByOpintooikeusId(fixture.studyRightId).single()
            assertThat(snapshot.id).isNotEqualTo(original.id)
            assertThat(payload.documents).hasSize(1)
            val identity = payload.documents.single()
            assertThat(identity.sourceDocumentId).isEqualTo(original.id)
            assertThat(identity.snapshotDocumentId).isEqualTo(snapshot.id)
            assertThat(identity.recordType).isEqualTo(reference.asiakirjatyyppi)
            assertThat(identity.jarjestysnumero).isEqualTo(reference.jarjestysnumero)
            assertThat(snapshot.archiveSnapshot).isTrue()
            assertThat(snapshot.asiakirjaData!!.data).isEqualTo(original.asiakirjaData!!.data)
            assertThat(reference.sha256).isEqualTo(ArkistointiChecksum.sha256(snapshot.asiakirjaData!!.data!!))
            PdfDocument(PdfReader(ByteArrayInputStream(snapshot.asiakirjaData!!.data))).use { pdf ->
                assertThat(pdf.numberOfPages).isPositive()
            }
        }
        approve(fixture, type).andExpect(status().isBadRequest)
        assertThat(documentCount(fixture)).isEqualTo(2)
        assertThat(taskCount(job.id!!)).isEqualTo(1)
    }

    @ForEachKoejaksoCaseType
    fun `repeated archive call preserves job snapshots and schedule without orphan documents`(type: CaseType) {
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        val first = job(fixture, type)
        val jobId = requireNotNull(first.id)
        val jobsBefore = jobs.count()
        val documentSql = "select id from asiakirja where opintooikeus_id = ? order by id"
        val documentsBefore = jdbc.queryForList(documentSql, Long::class.java, fixture.studyRightId)
        val referenceSql = "select * from arkistointi_job_asiakirja where job_id = ?"
        val referenceBefore = jdbc.queryForMap(referenceSql, jobId)
        val scheduleSql = "select * from scheduled_tasks where task_name = ? and task_instance = ?"
        val scheduleBefore = jdbc.queryForMap(scheduleSql, ARKISTOINTI_JOB_TASK.taskName, jobId.toString())

        // Call the archive service directly: the HTTP approval guard would reject this before reaching it.
        transactions.executeWithoutResult {
            val source = asiakirjaRepository.findAllByOpintooikeusId(fixture.studyRightId).single()
            if (type == CaseType.SOPIMUS) {
                archiveService.archive(requireNotNull(koulutussopimusRepository.findByIdForUpdate(fixture.koulutussopimusId)), source)
            } else {
                archiveService.archive(requireNotNull(vastuuhenkilonArvioRepository.findByIdForUpdate(fixture.vastuuhenkilonArvioId)), source)
            }
        }

        assertThat(jdbc.queryForList(documentSql, Long::class.java, fixture.studyRightId)).isEqualTo(documentsBefore)
        val repeated = job(fixture, type)
        assertThat(repeated.id).isEqualTo(jobId)
        assertThat(repeated.payload).isEqualTo(first.payload)
        assertThat(repeated.status).isEqualTo(first.status)
        assertThat(jobs.count()).isEqualTo(jobsBefore)
        assertThat(jdbc.queryForMap(referenceSql, jobId)).isEqualTo(referenceBefore)
        assertThat(jdbc.queryForMap(scheduleSql, ARKISTOINTI_JOB_TASK.taskName, jobId.toString())).isEqualTo(scheduleBefore)
    }

    @Test
    fun `negative final assessment is archived but return for correction is not`() {
        val rejected = fixture()
        approve(rejected, CaseType.KOEJAKSO, accepted = false).andExpect(status().isOk)
        val payload = objectMapper.readTree(job(rejected, CaseType.KOEJAKSO).payload)
        assertThat(payload.path("koejaksoHyvaksytty").booleanValue()).isFalse()
        val returned = fixture()
        approve(returned, CaseType.KOEJAKSO, correction = "Korjattava").andExpect(status().isOk)
        assertNoJob(returned, CaseType.KOEJAKSO)
        assertThat(documentCount(returned)).isZero()
    }

    @Test
    fun `contract return and resubmission can still be approved`() {
        val fixture = fixture()
        approve(fixture, CaseType.SOPIMUS, correction = "Korjattava").andExpect(status().isOk)
        val returned = koulutussopimusDto(fixture)
        assertThat(returned.lahetetty).isFalse()
        assertThat(returned.erikoistuvanAllekirjoitusaika).isNull()
        assertThat(returned.kouluttajat!!.single().sopimusHyvaksytty).isFalse()
        assertAwaitingFinalApproval(fixture, CaseType.SOPIMUS)

        setSecurityContext(fixture.traineeId, ERIKOISTUVA_LAAKARI)
        val resubmitted = koulutussopimusService.update(returned.apply { lahetetty = true }, fixture.traineeId)
        assertThat(resubmitted.lahetetty).isTrue()
        assertThat(resubmitted.erikoistuvanAllekirjoitusaika).isEqualTo(LocalDate.now())
        assertThat(resubmitted.kouluttajat!!.single().sopimusHyvaksytty).isFalse()
        assertAwaitingFinalApproval(fixture, CaseType.SOPIMUS)

        setSecurityContext(fixture.trainerId, KOULUTTAJA)
        val reviewed = koulutussopimusService.update(koulutussopimusDto(fixture), fixture.trainerId)
        assertThat(reviewed.kouluttajat!!.single().sopimusHyvaksytty).isTrue()
        assertThat(reviewed.vastuuhenkilonKorjausehdotus).isNull()
        assertAwaitingFinalApproval(fixture, CaseType.SOPIMUS)

        approve(fixture, CaseType.SOPIMUS).andExpect(status().isOk)
        assertSingleDelivery(fixture, CaseType.SOPIMUS)
    }

    @Test
    fun `assessment return and resubmission can still be approved`() {
        val fixture = fixture()
        approve(fixture, CaseType.KOEJAKSO, correction = "Korjattava").andExpect(status().isOk)
        transactions.executeWithoutResult {
            val returned = vastuuhenkilonArvioRepository.findById(fixture.vastuuhenkilonArvioId).orElseThrow()
            assertThat(returned.erikoistuvanKuittausaika).isNull()
            assertThat(returned.virkailijaHyvaksynyt).isFalse()
            assertThat(returned.virkailijanKuittausaika).isNull()
        }
        assertAwaitingFinalApproval(fixture, CaseType.KOEJAKSO)

        setSecurityContext(fixture.traineeId, ERIKOISTUVA_LAAKARI)
        vastuuhenkilonArvioService.update(vastuuhenkilonArvioDto(fixture), fixture.traineeId)
        transactions.executeWithoutResult {
            val resubmitted = vastuuhenkilonArvioRepository.findById(fixture.vastuuhenkilonArvioId).orElseThrow()
            assertThat(resubmitted.erikoistuvanKuittausaika).isEqualTo(LocalDate.now())
            assertThat(resubmitted.virkailijaHyvaksynyt).isFalse()
        }
        assertAwaitingFinalApproval(fixture, CaseType.KOEJAKSO)

        setSecurityContext(fixture.reviewerId, OPINTOHALLINNON_VIRKAILIJA)
        vastuuhenkilonArvioService.update(vastuuhenkilonArvioDto(fixture).apply { virkailijanKorjausehdotus = null }, fixture.reviewerId)
        transactions.executeWithoutResult {
            val reviewed = vastuuhenkilonArvioRepository.findById(fixture.vastuuhenkilonArvioId).orElseThrow()
            assertThat(reviewed.virkailijaHyvaksynyt).isTrue()
            assertThat(reviewed.virkailijanKuittausaika).isEqualTo(LocalDate.now())
            assertThat(reviewed.vastuuhenkilonKorjausehdotus).isNull()
        }
        assertAwaitingFinalApproval(fixture, CaseType.KOEJAKSO)

        approve(fixture, CaseType.KOEJAKSO).andExpect(status().isOk)
        assertSingleDelivery(fixture, CaseType.KOEJAKSO)
    }

    @ForEachKoejaksoCaseType
    fun `concurrent approvals serialize and produce one delivery`(type: CaseType) {
        val fixture = fixture()
        val dto = koulutussopimusDto(fixture)
        val vastuuhenkilonArvioDto = vastuuhenkilonArvioDto(fixture).apply { koejaksoHyvaksytty = true }
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results = (1..2).map {
                executor.submit<Boolean> {
                    start.await(10, TimeUnit.SECONDS)
                    try {
                        setSecurityContext(fixture.approverId, VASTUUHENKILO)
                        if (type == CaseType.SOPIMUS) koulutussopimusService.update(dto, fixture.approverId)
                        else vastuuhenkilonArvioService.update(vastuuhenkilonArvioDto, fixture.approverId, null, null)
                        true
                    } catch (_: BadRequestAlertException) {
                        false
                    }
                }
            }
            start.countDown()
            assertThat(results.map { it.get(60, TimeUnit.SECONDS) }).containsExactlyInAnyOrder(true, false)
            assertThat(documentCount(fixture)).isEqualTo(2)
            assertThat(taskCount(job(fixture, type).id!!)).isEqualTo(1)
            assertThat(mockingDetails(mail).invocations.count { it.method.name == "sendEmailFromTemplate" })
                .isEqualTo(if (type == CaseType.SOPIMUS) 1 else 2)
        } finally {
            executor.shutdownNow()
        }
    }

    @ForEachKoejaksoCaseType
    fun `processor delivers saved identities and bytes after the original document is replaced`(type: CaseType) {
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        val saved = job(fixture, type)
        val identity = ArchivePayloadReader(objectMapper).read(saved.payload!!).documents.single()
        transactions.executeWithoutResult {
            val studyRight = opintooikeusRepository.findById(fixture.studyRightId).orElseThrow()
            studyRight.opiskelijatunnus = "changed-after-approval"
            val source = asiakirjaRepository.findAllByOpintooikeusId(fixture.studyRightId).single()
            assertThat(source.id).isEqualTo(identity.sourceDocumentId).isNotEqualTo(identity.snapshotDocumentId)
            val replacement = source.copy(id = null, nimi = "replacement-original.pdf",
                asiakirjaData = AsiakirjaData(data = "changed-original-document".toByteArray()))
            asiakirjaRepository.delete(source)
            asiakirjaRepository.save(replacement)
            assertThat(replacement.id).isNotEqualTo(identity.sourceDocumentId).isNotEqualTo(identity.snapshotDocumentId)
        }
        assertThat(asiakirjaRepository.existsById(identity.sourceDocumentId)).isFalse()
        var submission: ArkistointiSubmission? = null
        adapter.behavior = {
            submission = it
            ArkistointiResult.Success("external-koejakso")
        }
        // Simulate db-scheduler claiming this test's row; recovery/failure handlers are covered by worker ITs.
        processor.process(claim(saved.id!!))
        val delivered = requireNotNull(submission)
        assertThat(delivered.key).isEqualTo(saved.id)
        assertThat(delivered.caseType).isEqualTo(type)
        assertThat(delivered.payload).isEqualTo(saved.payload)
        assertThat(ArchivePayloadReader(objectMapper).read(delivered.payload).documents).containsExactly(identity)
        assertThat(delivered.asiakirjat).hasSize(1)
        assertThat(delivered.asiakirjat.single().asiakirjaId).isEqualTo(identity.snapshotDocumentId)
        assertThat(ArkistointiChecksum.sha256(delivered.asiakirjat.single().sisalto))
            .isEqualTo(delivered.asiakirjat.single().sha256)
        PdfDocument(PdfReader(ByteArrayInputStream(delivered.asiakirjat.single().sisalto))).use {
            assertThat(it.numberOfPages).isPositive()
        }
        assertThat(jobs.findById(saved.id!!).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
    }

    @Test
    fun `remote failure cannot roll back committed approval`() {
        val fixture = fixture()
        approve(fixture, CaseType.SOPIMUS).andExpect(status().isOk)
        val saved = job(fixture, CaseType.SOPIMUS)
        adapter.behavior = { ArkistointiResult.RetryableFailure(ArkistointiError.create("TEST_OFFLINE", "Offline")) }
        val execution = claim(saved.id!!)
        assertThrows<ArkistointiProcessingException> {
            processor.process(execution)
        }
        assertThat(koulutussopimusRepository.findById(fixture.koulutussopimusId).orElseThrow().vastuuhenkiloHyvaksynyt).isTrue()
    }

    private fun assertSingleDelivery(fixture: Fixture, type: CaseType) {
        val job = job(fixture, type)
        assertThat(taskCount(job.id!!)).isEqualTo(1)
        assertThat(documentCount(fixture)).isEqualTo(2)
        approve(fixture, type).andExpect(status().isBadRequest)
        assertThat(taskCount(job.id!!)).isEqualTo(1)
        assertThat(documentCount(fixture)).isEqualTo(2)
        verify(creator).create(any())
        transactions.executeWithoutResult {
            verify(scheduler).schedule(eq(job.id!!), any())
        }
    }

    private fun claim(id: UUID): ArkistointiJobExecution = transactions.execute {
        jdbc.update("update scheduled_tasks set picked = true where task_instance = ?", id.toString())
        val version = jdbc.queryForObject("select version from scheduled_tasks where task_instance = ?", Long::class.java, id.toString())!!
        ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, id.toString(), version)
    }
}
