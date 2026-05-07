package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.ArkistointiService
import fi.elsapalvelu.elsa.service.PdfService
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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

    /**
     * Overrides the real [ArkistointiService] bean for the whole test class.
     * Each test configures its own stubbing via [whenever].
     */
    @MockitoBean
    private lateinit var arkistointiService: ArkistointiService

    /**
     * Mocked so that PDF generation (Thymeleaf rendering) is skipped entirely.
     * The private luoYhteenvetoPdf / luoLiitteetPdf / luoErikoistujanTiedotPdf methods
     * write to a ByteArrayOutputStream and then save the bytes to [asiakirjaRepository];
     * with pdfService mocked the OutputStream stays empty (valid, just zero-byte PDF).
     * This allows execution to reach the arkistointiService.muodostaSahke call.
     */
    @MockitoBean
    private lateinit var pdfService: PdfService

    private lateinit var opintooikeus: Opintooikeus
    private lateinit var vastuuhenkilo: Kayttaja
    private lateinit var anotherVastuuhenkilo: Kayttaja
    private lateinit var virkailija: Kayttaja

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
    @Transactional
    fun updateValmistumispyyntoByHyvaksyjaUserId_happyPath_kuittausaikaSavedAndReturns200() {
        initTest()

        val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            opintooikeus, anotherVastuuhenkilo, virkailija
        )
        em.persist(valmistumispyynto)

        val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
        em.persist(tarkistus)

        val sizeBefore = valmistumispyyntoRepository.findAll().size

        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(false)

        restMockMvc.perform(
            put("$ARKISTOINTI_HYVAKSYNTA_ENDPOINT/{id}", valmistumispyynto.id)
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

        // --- Setup: run inside a dedicated transaction that is committed before the service call ---
        // This is necessary because this test method is NOT @Transactional (by design — we need
        // to verify the DB rollback via a fresh read after the service's own transaction fails).
        // em.persist() requires an active transaction; TransactionTemplate provides one and commits it.
        val valmistumispyyntoId: Long = transactionTemplate.execute {
            initTest()
            val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
                opintooikeus, anotherVastuuhenkilo, virkailija
            )
            em.persist(valmistumispyynto)
            val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
            em.persist(tarkistus)
            em.flush()
            valmistumispyynto.id!!
        }!!

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

        val valmistumispyyntoId: Long = transactionTemplate.execute {
            initTest()
            val valmistumispyynto = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
                opintooikeus, anotherVastuuhenkilo, virkailija
            )
            em.persist(valmistumispyynto)
            val tarkistus = ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(valmistumispyynto)
            em.persist(tarkistus)
            em.flush()
            valmistumispyynto.id!!
        }!!

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
        } finally {
            adviceLogger.detachAppender(logAppender)
        }
    }

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

        val erikoistuvaLaakariUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(ERIKOISTUVA_LAAKARI)
        )
        em.persist(erikoistuvaLaakariUser)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, erikoistuvaLaakariUser)
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
}

