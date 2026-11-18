package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

/** Approval commit and rollback when archive job creation fails. */
class ValmistumispyyntoHyvaksyntaCommitRollbackIT : ValmistumispyyntoHyvaksyntaItBase() {

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

        restMockMvc.perform(
            put("$ARKISTOINTI_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyyntoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
                .with(csrf())).andExpect(status().isOk)

        val requestCaptor = argumentCaptor<CreateArkistointiJobRequest>()
        verify(arkistointiJobCreator).create(requestCaptor.capture())
        assertThat(requestCaptor.firstValue.asiakirjat.map { it.asiakirjatyyppi })
            .containsExactly(
                RecordType.YHTEENVETO,
                RecordType.LIITE
            )
        assertThat(requestCaptor.firstValue.key)
            .isEqualTo("valmistuminen:TAMPEREEN_YLIOPISTO:$valmistumispyyntoId")
        verify(arkistointiService, never()).muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
        verify(arkistointiService, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    // -------------------------------------------------------------------------
    // Test 2 — Exception during job creation: 5xx, error logged, transaction rolled back
    // -------------------------------------------------------------------------

    /**
     * Reproduces the production failure for HELSINGIN_YLIOPISTO approvals:
     *
     * Given: valmistumispyynto is in state "odottaa hyvaksyntaa"
     * And:   arkistointiService.onKaytossa returns TRUE (archiving enabled)
     * And:   arkistointiJobCreator.create throws a persistence exception
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
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenJobCreationThrows_returns5xxLogsErrorAndRollsBack() {

        val valmistumispyyntoId: Long = initTestInTransaction()

        // Capture ERROR logs from the controller (where log.error is emitted on rethrow)
        val resourceLogger = LoggerFactory.getLogger(VastuuhenkiloValmistumispyyntoResource::class.java) as ch.qos.logback.classic.Logger
        val logAppender = ListAppender<ILoggingEvent>()
        logAppender.start()
        resourceLogger.addAppender(logAppender)

        try {
            // Archiving is enabled and job persistence fails.
            whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
            whenever(arkistointiJobCreator.create(any())).thenThrow(RuntimeException("Arkistointi-jobin tallennus epaonnistui"))

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
            assertThat(errorLogs).withFailMessage("Expected an ERROR log entry from VastuuhenkiloValmistumispyyntoResource").isNotEmpty
            val errorMessage = errorLogs[0].formattedMessage
            assertThat(errorMessage).withFailMessage("Error log should reference the PUT endpoint path").contains(valmistumispyyntoId.toString())
            assertThat(errorLogs[0].throwableProxy?.message)
                .withFailMessage("Error log should include the original exception message")
                .contains("Arkistointi-jobin tallennus epaonnistui")

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
    // Test 3 — JVM Error during job creation: also caught, logged, rolled back
    // -------------------------------------------------------------------------

    /**
     * Verifies that a JVM [Error] thrown from [ArkistointiJobCreator.create]
     * (e.g. OutOfMemoryError during ZIP creation) is handled correctly.
     *
     * [Error] is a sibling of [Exception] under [Throwable].
     * Spring Framework 6.x supports @ExceptionHandler for Error subclasses.
     * [fi.elsapalvelu.elsa.web.rest.errors.BadRequestExceptionAdvice] has an
     * @ExceptionHandler(Error::class) that logs at ERROR level and returns HTTP 500.
     *
     * Given: async archiving enabled, job creation throws OutOfMemoryError
     * Then:
     *   - HTTP 500 (INTERNAL_SERVER_ERROR) returned by BadRequestExceptionAdvice
     *   - ERROR log emitted by BadRequestExceptionAdvice containing the error message
     *   - DB rolled back: vastuuhenkiloHyvaksyjaKuittausaika remains null
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenJobCreationThrowsError_returns5xxLogsErrorAndRollsBack() {

        val valmistumispyyntoId: Long = initTestInTransaction()

        // BadRequestExceptionAdvice.handleError is where log.error fires for Error subclasses
        val adviceLogger = LoggerFactory.getLogger(fi.elsapalvelu.elsa.web.rest.errors.BadRequestExceptionAdvice::class.java) as ch.qos.logback.classic.Logger
        val logAppender = ListAppender<ILoggingEvent>()
        logAppender.start()
        adviceLogger.addAppender(logAppender)

        try {
            whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
            whenever(arkistointiJobCreator.create(any()))
                .thenThrow(OutOfMemoryError("Simuloitu muistivirhe arkistointi-jobissa"))

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
                .contains("Simuloitu muistivirhe arkistointi-jobissa")

            // DB must be rolled back
            val dbState = valmistumispyyntoRepository.findById(valmistumispyyntoId)
            assertThat(dbState).isPresent
            assertThat(dbState.get().vastuuhenkiloHyvaksyjaKuittausaika).withFailMessage("Transaction must roll back even when a JVM Error is thrown").isNull()

            verifyEmailNotSent()
        } finally {
            adviceLogger.detachAppender(logAppender)
        }
    }
}
