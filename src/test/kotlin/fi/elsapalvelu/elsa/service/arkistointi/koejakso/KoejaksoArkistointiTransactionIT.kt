package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobExecution
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessor
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiProcessingException
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.koejakso.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.koejakso.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.cache.Caching
import org.assertj.core.api.Assertions.assertThat
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate

/** Commits to a disposable PostgreSQL DB, using real PDFs, snapshots, creator and transactional scheduling. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///koejaksoArchive?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.tre.kaytossa=true",
    "application.arkistointi.tre.metadata.cases.koejakso.async-enabled=true",
    "application.arkistointi.tre.metadata.cases.sopimus.async-enabled=true",
    "application.arkistointi.tre.metadata.cases.sopimus.title=Koulutussopimus",
    "application.arkistointi.tre.metadata.cases.sopimus.type=Sopimus",
    "application.arkistointi.tre.metadata.cases.sopimus.documents.sopimus.retention-period=20",
    "db-scheduler.enabled=false"
])
@Import(KoejaksoArkistointiTransactionIT.TestConfig::class)
class KoejaksoArkistointiTransactionIT : ResourceIntegrationTestBase() {
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var properties: ApplicationProperties
    @MockitoSpyBean private lateinit var contracts: KoejaksonKoulutussopimusRepository
    @MockitoSpyBean private lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired private lateinit var contractMapper: KoejaksonKoulutussopimusMapper
    @Autowired private lateinit var assessmentMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var contractService: KoejaksonKoulutussopimusService
    @Autowired private lateinit var assessmentService: KoejaksonVastuuhenkilonArvioService
    @Autowired private lateinit var archiveService: KoejaksoArkistointiService
    @Autowired private lateinit var processor: ArkistointiJobProcessor
    @MockitoSpyBean private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @MockitoSpyBean private lateinit var scheduler: ArkistointiJobScheduler
    @MockitoSpyBean private lateinit var archiveMapper: ObjectMapper
    @MockitoSpyBean private lateinit var creator: ArkistointiJobCreator
    @MockitoSpyBean private lateinit var legacy: SynchronousArkistointiService
    @MockitoBean private lateinit var mail: MailService
    @Autowired private lateinit var adapter: TestAdapter

    @BeforeEach
    fun configure() {
        properties.getArkistointi().getTre().kaytossa = true
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(CaseType.KOEJAKSO)!!.asyncEnabled = true
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(CaseType.SOPIMUS)!!.asyncEnabled = true
        adapter.behavior = { ArkistointiResult.Success("external-koejakso") }
        doReturn(ArkistointiPackage("/tmp/koejakso-test.zip", null)).whenever(legacy).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )
        doAnswer { null }.whenever(legacy).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `checked payload serialization failure rolls back asynchronous approval and real PDF`(type: CaseType) {
        val fixture = fixture()
        doAnswer { throw JsonMappingException.fromUnexpectedIOE(IOException("Simulated serialization failure")) }
            .whenever(archiveMapper).writeValueAsString(isA<KoejaksoArkistointiPayload>())

        approve(fixture, type).andExpect(status().isInternalServerError)

        assertRolledBack(fixture, type)
        verify(snapshots).createReference(any(), any(), any())
        verifyNoInteractions(creator, scheduler)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `approval commits one immutable PDF and a scheduled job with approval time metadata`(type: CaseType) {
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        val job = job(fixture, type)
        assertThat(job.caseType).isEqualTo(type)
        assertThat(job.expectedDocumentCount).isEqualTo(1)
        assertThat(taskCount(job.id!!)).isEqualTo(1)
        val payload = objectMapper.readValue(job.payload, KoejaksoArkistointiPayload::class.java)
        assertThat(payload.approvalDate).isEqualTo(LocalDate.now())
        assertThat(payload.caseId).isEqualTo(fixture.caseId(type))
        assertThat(payload.opintooikeusId).isEqualTo(fixture.studyRightId)
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

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
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
            if (type == CaseType.SOPIMUS) {
                val contract = requireNotNull(contracts.findByIdForUpdate(fixture.contractId))
                val source = asiakirjaRepository.findAllByOpintooikeusId(fixture.studyRightId).single()
                archiveService.archive(contract, source)
            } else {
                val assessment = requireNotNull(assessments.findByIdForUpdate(fixture.assessmentId))
                val source = asiakirjaRepository.findAllByOpintooikeusId(fixture.studyRightId).single()
                archiveService.archive(assessment, source)
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
        val payload = objectMapper.readValue(job(rejected, CaseType.KOEJAKSO).payload, KoejaksoArkistointiPayload::class.java)
        assertThat(payload.koejaksoHyvaksytty).isFalse()
        val returned = fixture()
        approve(returned, CaseType.KOEJAKSO, correction = "Korjattava").andExpect(status().isOk)
        assertNoJob(returned, CaseType.KOEJAKSO)
        assertThat(documentCount(returned)).isZero()
    }

    @Test
    fun `contract return and resubmission can still be approved`() {
        val fixture = fixture()
        approve(fixture, CaseType.SOPIMUS, correction = "Korjattava").andExpect(status().isOk)
        val returned = contractDto(fixture)
        assertThat(returned.lahetetty).isFalse()
        assertThat(returned.erikoistuvanAllekirjoitusaika).isNull()
        assertThat(returned.kouluttajat!!.single().sopimusHyvaksytty).isFalse()
        assertAwaitingFinalApproval(fixture, CaseType.SOPIMUS)

        setSecurityContext(fixture.traineeId, ERIKOISTUVA_LAAKARI)
        val resubmitted = contractService.update(returned.apply { lahetetty = true }, fixture.traineeId)
        assertThat(resubmitted.lahetetty).isTrue()
        assertThat(resubmitted.erikoistuvanAllekirjoitusaika).isEqualTo(LocalDate.now())
        assertThat(resubmitted.kouluttajat!!.single().sopimusHyvaksytty).isFalse()
        assertAwaitingFinalApproval(fixture, CaseType.SOPIMUS)

        setSecurityContext(fixture.trainerId, KOULUTTAJA)
        val reviewed = contractService.update(contractDto(fixture), fixture.trainerId)
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
            val returned = assessments.findById(fixture.assessmentId).orElseThrow()
            assertThat(returned.erikoistuvanKuittausaika).isNull()
            assertThat(returned.virkailijaHyvaksynyt).isFalse()
            assertThat(returned.virkailijanKuittausaika).isNull()
        }
        assertAwaitingFinalApproval(fixture, CaseType.KOEJAKSO)

        setSecurityContext(fixture.traineeId, ERIKOISTUVA_LAAKARI)
        assessmentService.update(assessmentDto(fixture), fixture.traineeId)
        transactions.executeWithoutResult {
            val resubmitted = assessments.findById(fixture.assessmentId).orElseThrow()
            assertThat(resubmitted.erikoistuvanKuittausaika).isEqualTo(LocalDate.now())
            assertThat(resubmitted.virkailijaHyvaksynyt).isFalse()
        }
        assertAwaitingFinalApproval(fixture, CaseType.KOEJAKSO)

        setSecurityContext(fixture.reviewerId, OPINTOHALLINNON_VIRKAILIJA)
        assessmentService.update(assessmentDto(fixture).apply { virkailijanKorjausehdotus = null }, fixture.reviewerId)
        transactions.executeWithoutResult {
            val reviewed = assessments.findById(fixture.assessmentId).orElseThrow()
            assertThat(reviewed.virkailijaHyvaksynyt).isTrue()
            assertThat(reviewed.virkailijanKuittausaika).isEqualTo(LocalDate.now())
            assertThat(reviewed.vastuuhenkilonKorjausehdotus).isNull()
        }
        assertAwaitingFinalApproval(fixture, CaseType.KOEJAKSO)

        approve(fixture, CaseType.KOEJAKSO).andExpect(status().isOk)
        assertSingleDelivery(fixture, CaseType.KOEJAKSO)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `failure after real scheduling rolls back the scheduler row with approval and documents`(type: CaseType) {
        val fixture = fixture()
        var scheduledJobId: UUID? = null
        transactions.executeWithoutResult {
            doAnswer { invocation ->
                val jobId = invocation.getArgument<UUID>(0)
                assertThat(invocation.callRealMethod()).isEqualTo(true)
                assertThat(taskCount(jobId)).isEqualTo(1)
                scheduledJobId = jobId
                error("Failure after inserting the real scheduler row")
            }.whenever(scheduler).schedule(any(), any())
        }

        approve(fixture, type).andExpect(status().isInternalServerError)

        assertThat(scheduledJobId).isNotNull()
        // Query the captured UUID directly: a join with the rolled-back job would hide an orphan task.
        assertThat(taskCount(requireNotNull(scheduledJobId))).isZero()
        assertThat(jobs.findById(requireNotNull(scheduledJobId))).isEmpty()
        assertRolledBack(fixture, type)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `scheduler failure rolls back approval documents and notifications`(type: CaseType) {
        val fixture = fixture()
        transactions.executeWithoutResult {
            doReturn(false).whenever(scheduler).schedule(any(), any())
        }
        approve(fixture, type).andExpect(status().isInternalServerError)
        assertRolledBack(fixture, type)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `snapshot failure is safely reported and rolls back`(type: CaseType) {
        val fixture = fixture()
        doThrow(ArkistointiAsiakirjaException(ArkistointiError.create("ARCHIVE_DOCUMENT_INVALID", "unsafe.pdf")))
            .whenever(snapshots).createReference(any(), any(), any())
        approve(fixture, type).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.errorKey").value("ARCHIVE_DOCUMENT_INVALID"))
            .andExpect(jsonPath("$.detail").value("Arkistoitavien asiakirjojen kasittely epaonnistui."))
        assertRolledBack(fixture, type)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `input document rejection has safe 400 contract and rolls back`(type: CaseType) {
        val fixture = fixture()
        doThrow(ArkistointiAsiakirjaException(
            ArkistointiError.create("ARCHIVE_DOCUMENT_INVALID", "unsafe.pdf"), ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT
        )).whenever(snapshots).createReference(any(), any(), any())
        approve(fixture, type).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("error.ARCHIVE_DOCUMENT_INVALID"))
            .andExpect(jsonPath("$.params").doesNotExist())
            .andExpect(jsonPath("$.detail").value("Arkistoitava lahdeasiakirja puuttuu tai sita ei voida kasitella."))
        assertRolledBack(fixture, type)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `creator failure rolls back approval and snapshot`(type: CaseType) {
        val fixture = fixture()
        doThrow(IllegalStateException("test failure")).whenever(creator).create(any())
        approve(fixture, type).andExpect(status().isInternalServerError)
        assertRolledBack(fixture, type)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `missing previous approval prevents final decision and creates no documents`(type: CaseType) {
        val fixture = fixture()
        transactions.executeWithoutResult {
            if (type == CaseType.SOPIMUS) {
                contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.forEach { trainer -> trainer.sopimusHyvaksytty = false }
            } else {
                assessments.findById(fixture.assessmentId).orElseThrow().virkailijaHyvaksynyt = false
            }
        }
        approve(fixture, type).andExpect(status().isBadRequest)
        assertRolledBack(fixture, type)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `case type opt in is independent and sync creates no archive job`(type: CaseType) {
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(type)!!.asyncEnabled = false
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        verify(legacy).laheta(any(), eq("/tmp/koejakso-test.zip"), eq(type), any(), anyOrNull(), anyOrNull())
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(1)
        val otherType = if (type == CaseType.SOPIMUS) CaseType.KOEJAKSO else CaseType.SOPIMUS
        approve(fixture, otherType).andExpect(status().isOk)
        assertThat(job(fixture, otherType).id).isNotNull()
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `sync service preserves approval without the new previous approval checks`(type: CaseType) {
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(type)!!.asyncEnabled = false
        val fixture = fixture()
        transactions.executeWithoutResult {
            if (type == CaseType.SOPIMUS) {
                contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.forEach { trainer -> trainer.sopimusHyvaksytty = false }
            } else {
                assessments.findById(fixture.assessmentId).orElseThrow().virkailijaHyvaksynyt = false
            }
        }

        // Existing HTTP guards still apply; exercise the service boundary changed by task5 directly.
        updateApproval(fixture, type)

        verify(legacy).laheta(any(), eq("/tmp/koejakso-test.zip"), eq(type), any(), anyOrNull(), anyOrNull())
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(1)
        verifyNoInteractions(snapshots, creator, scheduler)
        verify(contracts, never()).findByIdForUpdate(any())
        verify(assessments, never()).findByIdForUpdate(any())
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `sync service preserves repeated approval without the new async guard`(type: CaseType) {
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(type)!!.asyncEnabled = false
        val fixture = fixture()

        updateApproval(fixture, type)
        updateApproval(fixture, type)

        verify(legacy, times(2)).laheta(any(), eq("/tmp/koejakso-test.zip"), eq(type), any(), anyOrNull(), anyOrNull())
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(2)
        verifyNoInteractions(snapshots, creator, scheduler)
        verify(contracts, never()).findByIdForUpdate(any())
        verify(assessments, never()).findByIdForUpdate(any())
    }

    @Test
    fun `sync HTTP still rejects assessment reapproval as on main`() {
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(CaseType.KOEJAKSO)!!.asyncEnabled = false
        val fixture = fixture()

        approve(fixture, CaseType.KOEJAKSO).andExpect(status().isOk)
        approve(fixture, CaseType.KOEJAKSO).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("error.dataillegal.hyvaksyttya-arviointia-ei-saa-muokata"))

        assertThat(documentCount(fixture)).isEqualTo(1)
        assertNoJob(fixture, CaseType.KOEJAKSO)
        verifyNoInteractions(snapshots, creator, scheduler)
    }

    @Test
    fun `sync HTTP still requires trainer approvals for a contract as on main`() {
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(CaseType.SOPIMUS)!!.asyncEnabled = false
        val fixture = fixture()
        transactions.executeWithoutResult {
            contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.forEach { trainer -> trainer.sopimusHyvaksytty = false }
        }

        approve(fixture, CaseType.SOPIMUS).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(
                "error.dataillegal.vastuuhenkilo-ei-saa-muokata-sopimusta-jos-kouluttajat-eivat-ole-hyvaksyneet-sita"
            ))

        assertAwaitingFinalApproval(fixture, CaseType.SOPIMUS)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `disabled archiving still generates ordinary PDF without a snapshot`(type: CaseType) {
        val fixture = fixture()
        properties.getArkistointi().getTre().kaytossa = false
        approve(fixture, type).andExpect(status().isOk)
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(1)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `concurrent approvals serialize and produce one delivery`(type: CaseType) {
        val fixture = fixture()
        val dto = contractDto(fixture)
        val assessmentDto = assessmentDto(fixture).apply { koejaksoHyvaksytty = true }
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results = (1..2).map {
                executor.submit<Boolean> {
                    start.await(10, TimeUnit.SECONDS)
                    try {
                        setSecurityContext(fixture.approverId, VASTUUHENKILO)
                        if (type == CaseType.SOPIMUS) contractService.update(dto, fixture.approverId)
                        else assessmentService.update(assessmentDto, fixture.approverId, null, null)
                        true
                    } catch (_: fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException) {
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

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `processor delivers saved identities and bytes after the original document is replaced`(type: CaseType) {
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        val saved = job(fixture, type)
        val identity = objectMapper.readValue(saved.payload, KoejaksoArkistointiPayload::class.java).documents.single()
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
        assertThat(objectMapper.readValue(delivered.payload, KoejaksoArkistointiPayload::class.java).documents).containsExactly(identity)
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
        assertThat(contracts.findById(fixture.contractId).orElseThrow().vastuuhenkiloHyvaksynyt).isTrue()
    }

    private fun assertRolledBack(fixture: Fixture, type: CaseType) {
        transactions.executeWithoutResult {
            val approved = if (type == CaseType.SOPIMUS) contracts.findById(fixture.contractId).orElseThrow().vastuuhenkiloHyvaksynyt
            else assessments.findById(fixture.assessmentId).orElseThrow().vastuuhenkiloHyvaksynyt
            assertThat(approved).isFalse()
        }
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isZero()
        verifyNoInteractions(mail)
    }

    private fun job(fixture: Fixture, type: CaseType) = jobs.findByKey(key(fixture, type))!!
    private fun key(fixture: Fixture, type: CaseType): String {
        val prefix = if (type == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        return "$prefix:${YliopistoEnum.TAMPEREEN_YLIOPISTO.name}:${fixture.caseId(type)}"
    }
    private fun assertNoJob(fixture: Fixture, type: CaseType) {
        assertThat(jobs.findByKey(key(fixture, type))).isNull()
    }

    private fun assertAwaitingFinalApproval(fixture: Fixture, type: CaseType) {
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isZero()
        verifyNoInteractions(snapshots, creator, scheduler)
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
    private fun documentCount(fixture: Fixture) = jdbc.queryForObject("select count(*) from asiakirja where opintooikeus_id = ?", Int::class.java, fixture.studyRightId)
    private fun taskCount(id: UUID) = jdbc.queryForObject("select count(*) from scheduled_tasks where task_instance = ?", Int::class.java, id.toString())

    private fun claim(id: UUID): ArkistointiJobExecution = transactions.execute {
        jdbc.update("update scheduled_tasks set picked = true where task_instance = ?", id.toString())
        val version = jdbc.queryForObject("select version from scheduled_tasks where task_instance = ?", Long::class.java, id.toString())!!
        ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, id.toString(), version)
    }

    private fun updateApproval(fixture: Fixture, type: CaseType) {
        setSecurityContext(fixture.approverId, VASTUUHENKILO)
        if (type == CaseType.SOPIMUS) {
            contractService.update(contractDto(fixture), fixture.approverId)
        } else {
            assessmentService.update(assessmentDto(fixture).apply { koejaksoHyvaksytty = true }, fixture.approverId, null, null)
        }
    }

    private fun approve(fixture: Fixture, type: CaseType, accepted: Boolean = true, correction: String? = null): org.springframework.test.web.servlet.ResultActions {
        setSecurityContext(fixture.approverId, VASTUUHENKILO)
        val dto = if (type == CaseType.SOPIMUS) contractDto(fixture).apply { korjausehdotus = correction }
        else assessmentDto(fixture).apply { koejaksoHyvaksytty = accepted; vastuuhenkilonKorjausehdotus = correction }
        val endpoint = if (type == CaseType.SOPIMUS) "koulutussopimus" else "vastuuhenkilonarvio"
        return testMockMvc.perform(put("/api/vastuuhenkilo/koejakso/$endpoint")
            .contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(dto)).with(csrf()))
    }
    private fun contractDto(fixture: Fixture): KoejaksonKoulutussopimusDTO = transactions.execute {
        contractMapper.toDto(contracts.findById(fixture.contractId).orElseThrow())
    }
    private fun assessmentDto(fixture: Fixture): KoejaksonVastuuhenkilonArvioDTO = transactions.execute {
        assessmentMapper.toDto(assessments.findById(fixture.assessmentId).orElseThrow())
    }

    private fun fixture(): Fixture = transactions.execute {
        val approverUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO))
        em.persist(approverUser)
        em.flush()
        setSecurityContext(approverUser.id!!, VASTUUHENKILO)
        val university = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
        val traineeUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI))
        em.persist(traineeUser)
        val trainee = ErikoistuvaLaakariHelper.createEntity(em, user = traineeUser, yliopisto = university)
        val approver = createVastuuhenkiloForKoejakso(trainee, user = approverUser)
        val trainerUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(KOULUTTAJA)).also(em::persist)
        val trainer = KayttajaHelper.createEntity(em, trainerUser).also(em::persist)
        val supervisor = KayttajaHelper.createEntity(em).also(em::persist)
        val reviewerUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(OPINTOHALLINNON_VIRKAILIJA)).also(em::persist)
        val reviewer = KayttajaHelper.createEntity(em, reviewerUser).apply { yliopistot.add(university) }.also(em::persist)
        val phases = KoejaksonVaiheetHelper.persistKoejaksoVaiheet(em, trainee, trainer, supervisor, approver, university)
        phases.koulutussopimus.erikoistuvanAllekirjoitusaika = LocalDate.now()
        val assessment = KoejaksonVaiheetHelper.createVastuuhenkilonArvio(trainee, approver).apply {
            erikoistuvanKuittausaika = LocalDate.now()
            virkailija = reviewer
            virkailijaHyvaksynyt = true
            virkailijanKuittausaika = LocalDate.now()
        }
        em.persist(assessment)
        em.flush()
        Fixture(
            phases.koulutussopimus.id!!, assessment.id!!, trainee.getOpintooikeusKaytossa()!!.id!!,
            approverUser.id!!, traineeUser.id!!, trainerUser.id!!, reviewerUser.id!!
        )
    }

    private data class Fixture(
        val contractId: Long,
        val assessmentId: Long,
        val studyRightId: Long,
        val approverId: String,
        val traineeId: String,
        val trainerId: String,
        val reviewerId: String
    ) {
        fun caseId(type: CaseType) = if (type == CaseType.SOPIMUS) contractId else assessmentId
    }

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        fun archiveTestAdapter() = TestAdapter()

        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:koejakso-archive-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }

    class TestAdapter : ArkistointiAdapter {
        override val university = YliopistoEnum.TAMPEREEN_YLIOPISTO
        override val supportedCaseTypes = setOf(CaseType.SOPIMUS, CaseType.KOEJAKSO)
        var behavior: (ArkistointiSubmission) -> ArkistointiResult = { ArkistointiResult.Success("external-koejakso") }
        override fun arkistoi(pyynto: ArkistointiSubmission) = behavior(pyynto)
    }
}
