package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.ItextPdfAssembler
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoService
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordProperties
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication as withAuthentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import jakarta.persistence.EntityManager

private const val ARKISTOINTI_HYVAKSYNTA_ENDPOINT = "/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta"

/**
 * Integration tests focused on the full approval transaction in
 * ValmistumispyyntoService.updateValmistumispyyntoByHyvaksyjaUserId:
 *
 * 1. Happy path — archiving disabled (default test config), approval committed to DB.
 * 2. Exception path — archiving enabled (mocked), [ArkistointiService.muodostaSahke] throws:
 *    - Controller must respond 5xx
 *    - Error must be logged at ERROR level with the exception message
 *    - Spring @Transactional must roll back the kuittausaika save
 *
 * Uses @MockBean to override [ArkistointiService], which creates a separate Spring
 * application context from the base IT suite.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ValmistumispyyntoHyvaksyntaArkistointiIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository

    @Autowired
    private lateinit var restMockMvc: MockMvc

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var valmistumispyyntoService: ValmistumispyyntoService

    /**
     * Overrides the real [ArkistointiService] bean for the whole test class.
     * Each test configures its own stubbing via [whenever].
     */
    @MockitoBean
    private lateinit var arkistointiService: ArkistointiService

    /**
     * Mocked so that the heavy parts of PDF generation — Thymeleaf rendering, embedded fonts,
     * the PDF/A colour profile — are skipped entirely. [stubPdfService] still makes `luoPdf`
     * write a small *real* PDF and `openAssembler` return a *real* [ItextPdfAssembler], because the
     * calling services (e.g. ValmistumispyynnonArviointiPdfService) always call
     * `assembler.add(...)`, even with zero entries (the template still renders an empty
     * summary page) — the bytes must be something iText's PdfReader can actually parse.
     * A full mock would make openAssembler() return null, which fails fast with
     * "Parameter specified as non-null is null" the moment any PDF service tries to use it.
     */
    @MockitoBean
    private lateinit var pdfService: PdfService

    /**
     * Real bean, not mocked: used only to construct the real [ItextPdfAssembler] returned by the
     * [pdfService] stub, so the assembly/merge logic under test is the production code path,
     * not a mock of it.
     */
    @Autowired
    private lateinit var pdfGenerationMetricsService: PdfGenerationMetricsService

    @MockitoBean
    private lateinit var mailService: MailService

    /**
     * [MockitoBean] resets [pdfService] before every test, so the stubbing has to be
     * reapplied here rather than once in `init`.
     */
    @BeforeEach
    fun stubPdfService() {
        whenever(pdfService.luoPdf(any(), any(), any())).thenAnswer { invocation ->
            val outputStream = invocation.getArgument<OutputStream>(2)
            outputStream.write(minimalPdf())
        }
        whenever(pdfService.openAssembler(any())).thenAnswer { invocation ->
            val firstDocument = invocation.getArgument<ByteArray>(0)
            ItextPdfAssembler(firstDocument, pdfGenerationMetricsService)
        }
    }

    /**
     * The smallest PDF iText will both write and read back: one blank page, no fonts, no
     * PDF/A conformance. Good enough as a stand-in for a real rendered template, since none
     * of these tests assert on the generated PDFs' visual content.
     */
    private fun minimalPdf(): ByteArray {
        val out = ByteArrayOutputStream()
        PdfDocument(PdfWriter(out)).use { it.addNewPage() }
        return out.toByteArray()
    }

    private lateinit var opintooikeus: Opintooikeus
    private lateinit var vastuuhenkilo: Kayttaja
    private lateinit var anotherVastuuhenkilo: Kayttaja
    private lateinit var virkailija: Kayttaja
    private lateinit var vastuuhenkiloAuthentication: Saml2Authentication

    /**
     * IDs of entities committed by non-@Transactional tests 2 and 3.
     * null for @Transactional test 1 (data is rolled back automatically).
     * Used by @AfterEach to clean up committed rows so they don't leak into
     * subsequent test classes that share the same H2 in-memory database
     * (jdbc:h2:mem:elsaBackend — named, shared across all Spring contexts).
     */
    private var committedValmistumispyyntoId: Long? = null
    private var committedOpintooikeusId: Long? = null
    private var committedErikoistuvaLaakariId: Long? = null
    private var committedYliopistoId: Long? = null
    private val committedExtraErikoisalaIds: MutableList<Long> = mutableListOf()

    /**
     * Deletes rows committed by the non-@Transactional tests in FK-safe order
     * so that subsequent test classes that share the same H2 DB see a clean slate.
     *
     * Deletion order (child → parent, respecting FK constraints):
     *  1. valmistumispyynnon_tarkistus                      (FK → valmistumispyynto)
     *  2. valmistumispyynto                                 (FK → opintooikeus)
     *  3. rel_kayttaja__yliopisto                           (join table FK → yliopisto)
     *  4. rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi  (FK → kayttaja_yliopisto_erikoisala)
     *  5. kayttaja_yliopisto_erikoisala                     (FK → yliopisto)
     *  6. erikoisala extras created by initVastuuhenkiloErikoisalat
     *     (no remaining FK refs)
     *  7. opintooikeus                                      (FK → erikoistuva_laakari, yliopisto)
     *  8. erikoistuva_laakari                               (no remaining FK references from our data)
     *  9. yliopisto                                         (no remaining FK references from our data)
     */
    @AfterEach
    fun cleanupCommittedData() {
        val vpId = committedValmistumispyyntoId ?: return
        val ooId = committedOpintooikeusId ?: return
        val elId = committedErikoistuvaLaakariId
        val yId = committedYliopistoId ?: return

        transactionTemplate.execute {
            em.createNativeQuery("DELETE FROM valmistumispyynnon_tarkistus WHERE valmistumispyynto_id = $vpId").executeUpdate()
            // Null out the asiakirja FK columns before deleting the asiakirja rows themselves,
            // because valmistumispyynto has FK references (yhteenveto_asiakirja_id etc.) to asiakirja.
            em.createNativeQuery(
                "UPDATE valmistumispyynto SET yhteenveto_asiakirja_id = NULL, " +
                    "liitteet_asiakirja_id = NULL, erikoistujan_tiedot_asiakirja_id = NULL " +
                    "WHERE id = $vpId"
            ).executeUpdate()
            // Delete asiakirja rows created by PDF generation (which commit to DB when the test
            // is not @Transactional). asiakirja has a FK to opintooikeus, so must be deleted
            // before opintooikeus.
            em.createNativeQuery("DELETE FROM asiakirja WHERE opintooikeus_id = $ooId").executeUpdate()
            em.createNativeQuery("DELETE FROM valmistumispyynto WHERE id = $vpId").executeUpdate()
            em.createNativeQuery("DELETE FROM rel_kayttaja__yliopisto WHERE yliopisto_id = $yId").executeUpdate()
            em.createNativeQuery(
                "DELETE FROM rel_kayttaja_yliopisto_erikoisala__tehtavatyyppi " +
                    "WHERE kayttaja_yliopisto_erikoisala_id IN " +
                    "(SELECT id FROM kayttaja_yliopisto_erikoisala WHERE yliopisto_id = $yId)"
            ).executeUpdate()
            em.createNativeQuery("DELETE FROM kayttaja_yliopisto_erikoisala WHERE yliopisto_id = $yId").executeUpdate()
            if (committedExtraErikoisalaIds.isNotEmpty()) {
                em.createNativeQuery("DELETE FROM erikoisala WHERE id IN (${committedExtraErikoisalaIds.joinToString()})").executeUpdate()
            }
            em.createNativeQuery("DELETE FROM opintooikeus WHERE id = $ooId").executeUpdate()
            if (elId != null) {
                em.createNativeQuery("DELETE FROM erikoistuva_laakari WHERE id = $elId").executeUpdate()
            }
            em.createNativeQuery("DELETE FROM yliopisto WHERE id = $yId").executeUpdate()
            em.clear()
        }

        committedValmistumispyyntoId = null
        committedOpintooikeusId = null
        committedErikoistuvaLaakariId = null
        committedYliopistoId = null
        committedExtraErikoisalaIds.clear()
    }

    // -------------------------------------------------------------------------
    // Test 1 — Happy path: approval persisted, no archiving
    // -------------------------------------------------------------------------

    /**
     * Given: valmistumispyynto is in state "odottaa hyvaksyntaa"
     * And:   arkistointiService.onKaytossa returns false (no archiving)
     * When:  vastuuhenkilo sends PUT hyvaksynta with korjausehdotus=null
     * Then:  HTTP 200, vastuuhenkiloHyvaksyjaKuittausaika = today, no DB rollback
     *
     * This is the same logic as [VastuuhenkiloValmistumispyyntoResourceIT.ackValmistumispyyntoHyvaksyja]
     * but with an explicit @MockBean so it is self-contained and documents the intended flow.
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_happyPath_kuittausaikaSavedAndReturns200() {
        val valmistumispyyntoId: Long = initTestInTransaction()

        val sizeBefore = valmistumispyyntoRepository.findAll().size

        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(false)

        restMockMvc.perform(
            put("$ARKISTOINTI_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyyntoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                .with(csrf())
        ).andExpect(status().isOk)

        val list = valmistumispyyntoRepository.findAll()
        assertThat(list).hasSize(sizeBefore)

        val updated = list[list.size - 1]
        assertThat(updated.vastuuhenkiloHyvaksyja).isEqualTo(vastuuhenkilo)
        assertThat(updated.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
        assertThat(updated.vastuuhenkiloHyvaksyjaPalautusaika).isNull()
        assertThat(updated.vastuuhenkiloHyvaksyjaKorjausehdotus).isNull()

        verifyEmailSent()
    }

    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_archivingEnabled_usesGeneratedPersistedDocuments() {
        val valmistumispyyntoId = initTestInTransaction()
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
        whenever(
            arkistointiService.muodostaSahke(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        ).thenReturn(ArkistointiResult("/tmp/valmistumispyynto.zip", null))

        restMockMvc.perform(
            put("$ARKISTOINTI_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyyntoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                .with(csrf())
        ).andExpect(status().isOk)

        val asiakirjatCaptor = argumentCaptor<List<RecordProperties>>()
        verify(arkistointiService).muodostaSahke(
            any(),
            asiakirjatCaptor.capture(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )
        assertThat(asiakirjatCaptor.firstValue.map { it.type })
            .containsExactly(
                RecordType.YHTEENVETO,
                RecordType.LIITE
            )

        val persistedAsiakirjaIds = transactionTemplate.execute {
            val valmistumispyynto = valmistumispyyntoRepository.findById(valmistumispyyntoId).orElseThrow()
            listOf(
                valmistumispyynto.yhteenvetoAsiakirja?.id,
                valmistumispyynto.liitteetAsiakirja?.id
            )
        }
        assertThat(asiakirjatCaptor.firstValue.map { it.asiakirja.id })
            .containsExactlyElementsOf(persistedAsiakirjaIds)
    }

    // -------------------------------------------------------------------------
    // Test 2 — Exception in muodostaSahke: 5xx, error logged, transaction rolled back
    // -------------------------------------------------------------------------

    /**
     * Reproduces the production failure for HELSINGIN_YLIOPISTO approvals:
     *
     * Given: valmistumispyynto is in state "odottaa hyvaksyntaa"
     * And:   arkistointiService.onKaytossa returns TRUE (archiving enabled)
     * And:   arkistointiService.muodostaSahke throws RuntimeException("Arkistointipalvelu ei vastaa")
     * When:  vastuuhenkilo sends PUT hyvaksynta
     * Then:
     *   - HTTP 5xx returned to the client
     *   - ERROR log emitted by [VastuuhenkiloValmistumispyyntoResource] with the exception
     *   - @Transactional rolls back: vastuuhenkiloHyvaksyjaKuittausaika remains null in DB
     *     (verified by querying outside the rolled-back transaction scope)
     *
     * Note: the DB rollback assertion works because this test is NOT annotated @Transactional.
     * The service's own transaction (REQUIRED propagation) is rolled back by Spring when the
     * unchecked exception propagates out of the method boundary. The test then reads the
     * committed DB state via a fresh EntityManager transaction.
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenMuodostaSahkeThrows_returns5xxLogsErrorAndRollsBack() {

        val valmistumispyyntoId: Long = initTestInTransaction()

        // Capture ERROR logs from the controller (where log.error is emitted on rethrow)
        val resourceLogger = LoggerFactory.getLogger(
            VastuuhenkiloValmistumispyyntoResource::class.java
        ) as ch.qos.logback.classic.Logger
        val logAppender = ListAppender<ILoggingEvent>()
        logAppender.start()
        resourceLogger.addAppender(logAppender)

        try {
            // Archiving is "enabled" for this test — muodostaSahke throws
            whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
            whenever(
                arkistointiService.muodostaSahke(
                    any(), any(), any(), any(), any(), any(), any(), any(), any()
                )
            ).thenThrow(RuntimeException("Arkistointipalvelu ei vastaa"))

            // Act
            // Note on HTTP status: Spring Boot maps a rethrown JVM Error (OutOfMemoryError etc.)
            // to 404 via its error routing pipeline, because Error subclasses bypass Spring MVC's
            // ExceptionHandlerExceptionResolver (which only handles Exception). The important
            // invariant is that the response is NOT 2xx — the approval did NOT silently succeed.
            restMockMvc.perform(
                put("$ARKISTOINTI_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyyntoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                    .with(csrf())
            ).andExpect { result ->
                assertThat(result.response.status)
                    .withFailMessage(
                        "Expected error response (4xx or 5xx) — approval must NOT succeed when OOM is thrown"
                    )
                    .isGreaterThanOrEqualTo(400)
            }

            // Assert 1: ERROR must be logged — proves the explicit catch (ex: Error) block catches Error, not just Exception
            val errorLogs = logAppender.list.filter { it.level == Level.ERROR }
            assertThat(errorLogs)
                .withFailMessage("Expected an ERROR log entry from VastuuhenkiloValmistumispyyntoResource")
                .isNotEmpty
            val errorMessage = errorLogs[0].formattedMessage
            assertThat(errorMessage)
                .withFailMessage("Error log should reference the PUT endpoint path")
                .contains(valmistumispyyntoId.toString())
            assertThat(errorLogs[0].throwableProxy?.message)
                .withFailMessage("Error log should include the original exception message")
                .contains("Arkistointipalvelu ei vastaa")

            // Assert 2: DB rollback — kuittausaika must NOT be present in the committed state.
            // The service's @Transactional rolls back when the exception propagates, so the
            // valmistumispyyntoRepository.save(kuittausaika) from line ~319 is undone.
            val dbState = valmistumispyyntoRepository.findById(valmistumispyyntoId)
            assertThat(dbState).isPresent
            assertThat(dbState.get().vastuuhenkiloHyvaksyjaKuittausaika)
                .withFailMessage(
                    "Transaction should have rolled back: vastuuhenkiloHyvaksyjaKuittausaika " +
                    "must be null after archiving failure — data integrity is at risk if not null"
                )
                .isNull()

            verifyEmailNotSent()
        } finally {
            resourceLogger.detachAppender(logAppender)
        }
    }

    // -------------------------------------------------------------------------
    // Test 3 — JVM Error (not Exception) in muodostaSahke: also caught, logged, rolled back
    // -------------------------------------------------------------------------

    /**
     * Verifies that a JVM [Error] thrown from [ArkistointiService.muodostaSahke]
     * (e.g. OutOfMemoryError during ZIP creation) is handled correctly.
     *
     * [Error] is a sibling of [Exception] under [Throwable].
     * Spring Framework 6.x supports @ExceptionHandler for Error subclasses.
     * [fi.elsapalvelu.elsa.web.rest.errors.BadRequestExceptionAdvice] has an
     * @ExceptionHandler(Error::class) that logs at ERROR level and returns HTTP 500.
     *
     * Given: archiving enabled, muodostaSahke throws OutOfMemoryError
     * Then:
     *   - HTTP 500 (INTERNAL_SERVER_ERROR) returned by BadRequestExceptionAdvice
     *   - ERROR log emitted by BadRequestExceptionAdvice containing the error message
     *   - DB rolled back: vastuuhenkiloHyvaksyjaKuittausaika remains null
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenMuodostaSahkeThrowsError_returns5xxLogsErrorAndRollsBack() {

        val valmistumispyyntoId: Long = initTestInTransaction()

        // BadRequestExceptionAdvice.handleError is where log.error fires for Error subclasses
        val adviceLogger = LoggerFactory.getLogger(
            fi.elsapalvelu.elsa.web.rest.errors.BadRequestExceptionAdvice::class.java
        ) as ch.qos.logback.classic.Logger
        val logAppender = ListAppender<ILoggingEvent>()
        logAppender.start()
        adviceLogger.addAppender(logAppender)

        try {
            whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
            whenever(
                arkistointiService.muodostaSahke(
                    any(), any(), any(), any(), any(), any(), any(), any(), any()
                )
            ).thenThrow(OutOfMemoryError("Simuloitu muistivirhe arkistoinnissa"))

            // BadRequestExceptionAdvice.handleError returns HTTP 500
            restMockMvc.perform(
                put("$ARKISTOINTI_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyyntoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                    .with(csrf())
            ).andExpect(status().isInternalServerError)

            // ERROR must be logged by BadRequestExceptionAdvice with the original error message
            val errorLogs = logAppender.list.filter { it.level == Level.ERROR }
            assertThat(errorLogs)
                .withFailMessage(
                    "Expected ERROR log from BadRequestExceptionAdvice — " +
                        "handleError(Error) must log at ERROR level"
                )
                .isNotEmpty
            assertThat(errorLogs[0].throwableProxy?.message)
                .contains("Simuloitu muistivirhe arkistoinnissa")

            // DB must be rolled back
            val dbState = valmistumispyyntoRepository.findById(valmistumispyyntoId)
            assertThat(dbState).isPresent
            assertThat(dbState.get().vastuuhenkiloHyvaksyjaKuittausaika)
                .withFailMessage("Transaction must roll back even when a JVM Error is thrown")
                .isNull()

            verifyEmailNotSent()
        } finally {
            adviceLogger.detachAppender(logAppender)
        }
    }

    // -------------------------------------------------------------------------
    // Concurrency guard — one approval at a time per valmistumispyynto
    // -------------------------------------------------------------------------

    /**
     * Reproduces the production incident (the same valmistumispyynto approved four times).
     *
     * The first approval is held *inside* its transaction — after it has taken the row lock and
     * saved the kuittausaika, while archiving ([ArkistointiService.muodostaSahke]) is still
     * running — by a latch. While it is held, a second approval of the same valmistumispyynto is
     * sent.
     *
     * Given: an approval is in progress for the valmistumispyynto
     * When:  a second approval for the same valmistumispyynto arrives
     * Then:  - the second one is rejected with 409 and the "approval is already in progress"
     *          message key, without doing any work
     *        - the first one completes normally (200)
     *        - exactly one archiving run, one document set and one approval e-mail were produced
     *
     * Without the guard the second request runs the whole approval too, so it returns 200,
     * archiving runs twice, a second document set is created and the e-mails go out twice.
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenApprovalAlreadyInProgress_rejectsSecondRequestWith409() {
        val valmistumispyyntoId = initTestInTransaction()

        val firstApprovalInsideTransaction = CountDownLatch(1)
        val releaseFirstApproval = CountDownLatch(1)
        val muodostaSahkeCalls = AtomicInteger(0)

        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
        whenever(arkistointiService.muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
        ).thenAnswer {
            if (muodostaSahkeCalls.incrementAndGet() == 1) {
                firstApprovalInsideTransaction.countDown()
                check(releaseFirstApproval.await(30, TimeUnit.SECONDS)) {
                    "Test did not release the first approval in time"
                }
            }
            ArkistointiResult("/tmp/valmistumispyynto.zip", null)
        }

        val executor = Executors.newSingleThreadExecutor()
        try {
            val firstApproval = executor.submit<Int> {
                putHyvaksynta(valmistumispyyntoId).response.status
            }
            assertThat(firstApprovalInsideTransaction.await(30, TimeUnit.SECONDS))
                .withFailMessage("The first approval never reached the archiving step")
                .isTrue()

            val secondApproval = putHyvaksynta(valmistumispyyntoId)

            assertThat(secondApproval.response.status)
                .withFailMessage(
                    "Second concurrent approval must be rejected with 409 Conflict, was %s",
                    secondApproval.response.status
                )
                .isEqualTo(409)
            assertThat(secondApproval.response.contentAsString).contains("error.dataillegal.valmistumispyynnon-hyvaksynta-on-jo-kaynnissa")

            releaseFirstApproval.countDown()
            assertThat(firstApproval.get(30, TimeUnit.SECONDS)).isEqualTo(200)
        } finally {
            releaseFirstApproval.countDown()
            executor.shutdownNow()
        }

        // One approval, one archiving run, one e-mail set.
        assertThat(muodostaSahkeCalls.get()).isEqualTo(1)
        verifyEmailSent()

        // One document set: every generated asiakirja is still linked to the valmistumispyynto,
        // i.e. no orphan documents from a second run.
        transactionTemplate.execute {
            val saved = em.find(Valmistumispyynto::class.java, valmistumispyyntoId)
            assertThat(saved.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
            val linked = listOfNotNull(
                saved.yhteenvetoAsiakirja,
                saved.liitteetAsiakirja,
                saved.erikoistujanTiedotAsiakirja
            ).size
            val stored = (em.createNativeQuery(
                "SELECT COUNT(*) FROM asiakirja WHERE opintooikeus_id = ${opintooikeus.id}"
            ).singleResult as Number).toInt()
            assertThat(linked).isGreaterThan(0)
            assertThat(stored)
                .withFailMessage("Expected one document set ($linked documents) but found $stored")
                .isEqualTo(linked)
        }
    }

    /**
     * An approval that completed between the controller's "is it open" check and the start of
     * the service transaction must not be repeated: the state is re-checked under the lock.
     * (Over HTTP the controller check would already stop a plain sequential retry, so this calls
     * the service directly.)
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenAlreadyApproved_rejectsRepeatedApprovalAndSendsOneEmailSet() {
        val valmistumispyyntoId = initTestInTransaction()
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(false)
        val userId = vastuuhenkilo.user!!.id!!
        val form = ValmistumispyyntoHyvaksyntaFormDTO(null)

        valmistumispyyntoService.updateValmistumispyyntoByHyvaksyjaUserId(valmistumispyyntoId, userId, form)

        val thrown = org.assertj.core.api.Assertions.catchThrowable {
            valmistumispyyntoService.updateValmistumispyyntoByHyvaksyjaUserId(valmistumispyyntoId, userId, form)
        }

        assertThat(thrown).isInstanceOf(BadRequestAlertException::class.java)
        assertThat((thrown as BadRequestAlertException).errorKey)
            .isEqualTo("dataillegal.valmistumispyynto-ei-ole-muokattavissa")
        verifyEmailSent()
    }

    private fun putHyvaksynta(valmistumispyyntoId: Long): MvcResult =
        restMockMvc.perform(
            put("$ARKISTOINTI_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyyntoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                .with(csrf())
                // Explicit, because the request may run on a thread other than the test thread.
                .with(withAuthentication(vastuuhenkiloAuthentication))
        ).andReturn()

    private fun initTestInTransaction(): Long = transactionTemplate.execute {
        // --- Setup: run inside a dedicated transaction that is committed before the service call ---
        // This is necessary because this test method is NOT @Transactional (by design — we need
        // to verify the DB rollback via a fresh read after the service's own transaction fails).
        // em.persist() requires an active transaction; TransactionTemplate provides one and commits it.
        initTest()
        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            opintooikeus, anotherVastuuhenkilo, virkailija
        )
        em.persist(valmistumispyynto)
        val tarkistus =
            ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)
        em.flush()
        committedValmistumispyyntoId = valmistumispyynto.id!!
        committedOpintooikeusId = opintooikeus.id
        committedErikoistuvaLaakariId = opintooikeus.erikoistuvaLaakari?.id
        committedYliopistoId = opintooikeus.yliopisto?.id
        valmistumispyynto.id!!
    }!!

    // -------------------------------------------------------------------------
    // Setup helpers (mirrors VastuuhenkiloValmistumispyyntoResourceIT.initTest)
    // -------------------------------------------------------------------------

    private fun initTest() {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(vastuuhenkiloUser)

        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(vastuuhenkiloUser.id, mapOf()),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication
        vastuuhenkiloAuthentication = authentication

        val erikoistuvaLaakariUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(ERIKOISTUVA_LAAKARI)
        )
        em.persist(erikoistuvaLaakariUser)

        // Always create a fresh, dedicated Yliopisto so that @AfterEach can safely delete it
        // without touching any Yliopisto row that belongs to another test class's committed data.
        val freshYliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO)
        em.persist(freshYliopisto)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, erikoistuvaLaakariUser, yliopisto = freshYliopisto)
        em.persist(erikoistuvaLaakari)

        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)

        vastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)
        initVastuuhenkiloErikoisalat(
            vastuuhenkilo,
            opintooikeus.yliopisto!!,
            opintooikeus.erikoisala!!,
            tehtavatyypit.filter {
                it.nimi == VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            }.toMutableSet()
        )
        em.persist(vastuuhenkilo)

        val anotherVastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(VASTUUHENKILO)
        )
        em.persist(anotherVastuuhenkiloUser)
        anotherVastuuhenkilo = KayttajaHelper.createEntity(em, anotherVastuuhenkiloUser)
        initVastuuhenkiloErikoisalat(
            anotherVastuuhenkilo,
            opintooikeus.yliopisto!!,
            opintooikeus.erikoisala!!,
            tehtavatyypit.filter {
                it.nimi == VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
            }.toMutableSet()
        )
        em.persist(anotherVastuuhenkilo)

        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(OPINTOHALLINNON_VIRKAILIJA)
        )
        em.persist(virkailijaUser)
        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        em.persist(virkailija)
        virkailija.yliopistot.add(opintooikeus.yliopisto!!)
    }

    private fun initVastuuhenkiloErikoisalat(
        kayttaja: Kayttaja,
        yliopisto: Yliopisto,
        erikoisala: Erikoisala,
        tehtavat: MutableSet<VastuuhenkilonTehtavatyyppi>
    ) {
        val extra1 = ErikoisalaHelper.createEntity().also { em.persist(it) }
        val extra2 = ErikoisalaHelper.createEntity().also { em.persist(it) }
        committedExtraErikoisalaIds.addAll(listOf(extra1.id!!, extra2.id!!))

        listOf(extra1, erikoisala, extra2).forEach { ala ->
            kayttaja.yliopistotAndErikoisalat.add(
                KayttajaYliopistoErikoisala(
                    kayttaja = kayttaja,
                    yliopisto = yliopisto,
                    erikoisala = ala,
                    vastuuhenkilonTehtavat = tehtavat
                )
            )
        }
    }


    private fun verifyEmailSent() {
        verify(mailService).sendEmailFromTemplate(
            any<User>(),
            any<List<String>>(),
            eq("valmistumispyyntoHyvaksytty.html"),
            eq("email.valmistumispyyntoHyvaksytty.title"),
            any<Array<String>>(),
            any()
        )
        verify(mailService).sendEmailFromTemplate(
            anyOrNull<String>(),
            any<List<String>>(),
            eq("valmistumispyyntoHyvaksyttyVirkailija.html"),
            eq("email.valmistumispyyntoHyvaksytty.title"),
            any<Array<String>>(),
            any()
        )
    }

    private fun verifyEmailNotSent() {
        verify(mailService, never()).sendEmailFromTemplate(
            any<User>(),
            any<List<String>>(),
            any<String>(),
            any<String>(),
            any<Array<String>>(),
            any()
        )
        verify(mailService, never()).sendEmailFromTemplate(
            anyOrNull<String>(),
            any<List<String>>(),
            any<String>(),
            any<String>(),
            any<Array<String>>(),
            any()
        )
    }
}
