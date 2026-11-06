package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Per-valmistumispyynto approval lock: what it blocks and what it must not block. */
class ValmistumispyyntoHyvaksyntaConcurrencyIT : ValmistumispyyntoHyvaksyntaItBase() {

    // -------------------------------------------------------------------------
    // Concurrency guard — one approval at a time per valmistumispyynto
    // -------------------------------------------------------------------------

    /**
     * Reproduces the production incident (the same valmistumispyynto approved four times).
     *
     * The first approval is held *inside* its transaction — after it has taken the row lock and
     * saved the kuittausaika, while job creation ([ArkistointiJobCreator.create]) is still
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
        val jobCreationCalls = AtomicInteger(0)

        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
        whenever(arkistointiJobCreator.create(any())).thenAnswer {
            if (jobCreationCalls.incrementAndGet() == 1) {
                firstApprovalInsideTransaction.countDown()
                check(releaseFirstApproval.await(30, TimeUnit.SECONDS)) {
                    "Test did not release the first approval in time"
                }
            }
            ArkistointiJob(id = UUID.randomUUID())
        }

        val executor = Executors.newSingleThreadExecutor()
        try {
            val firstApproval = executor.submit<Int> {
                putHyvaksynta(valmistumispyyntoId).response.status
            }
            assertThat(firstApprovalInsideTransaction.await(30, TimeUnit.SECONDS)).withFailMessage("The first approval never reached the archiving step")
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
        assertThat(jobCreationCalls.get()).isEqualTo(1)
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
        assertThat((thrown as BadRequestAlertException).errorKey).isEqualTo("dataillegal.valmistumispyynto-ei-ole-muokattavissa")
        verifyEmailSent()
    }


    // -------------------------------------------------------------------------
    // Concurrency guard — what the lock must NOT block
    // -------------------------------------------------------------------------

    /**
     * The lock is per valmistumispyynto, not global and not per approver.
     *
     * Given: the approval of student 1 is in progress (held inside its transaction, row lock taken)
     * When:  the SAME approver approves the valmistumispyynto of student 2
     * Then:  the second approval is not blocked or rejected: it completes with 200 while the first
     *        one is still running, and afterwards each valmistumispyynto has its own complete
     *        document set and its own e-mails (2 runs, 2 e-mail sets, no mixing of documents).
     *
     * The approver's contact details in the form are the stored ones on purpose: an approval that
     * *changes* them updates the approver's user row, which every concurrent approval by the same
     * approver then waits for until the first one commits.
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenApprovalOfAnotherStudentInProgress_runsInParallelWithoutBeingBlocked() {
        val firstId = initTestInTransaction()
        val secondId = initSecondStudentValmistumispyynto()
        val gate = gateFirstArchivingCall()
        val approver = vastuuhenkilo.user!!
        val unchangedContactDetails = ValmistumispyyntoHyvaksyntaFormDTO(null, approver.email, approver.phoneNumber)

        val executor = Executors.newSingleThreadExecutor()
        try {
            val firstApproval = executor.submit<Int> {
                putHyvaksynta(firstId, form = unchangedContactDetails).response.status
            }
            assertThat(gate.firstEntered.await(30, TimeUnit.SECONDS)).withFailMessage("The first approval never reached the archiving step").isTrue()

            val secondApproval = putHyvaksynta(secondId, form = unchangedContactDetails)

            assertThat(secondApproval.response.status)
                .withFailMessage(
                    "Approving a DIFFERENT valmistumispyynto must not be blocked or rejected by the lock " +
                        "of another one, but returned %s",
                    secondApproval.response.status
                ).isEqualTo(200)
            assertThat(firstApproval.isDone).withFailMessage("The first approval was supposed to still be running (parallel execution)").isFalse()

            gate.release.countDown()
            assertThat(firstApproval.get(30, TimeUnit.SECONDS)).isEqualTo(200)
        } finally {
            gate.release.countDown()
            executor.shutdownNow()
        }

        assertThat(gate.calls.get()).isEqualTo(2)
        verifyEmailSent(times = 2)
        assertOwnCompleteDocumentSet(firstId, opintooikeus.id!!)
        assertOwnCompleteDocumentSet(secondId, committedSecondOpintooikeusId!!)
    }

    /**
     * The lock belongs to the valmistumispyynto, so it is not something another person can use to
     * disturb a running approval: a person who is not the approver of the valmistumispyynto is
     * turned away by the authorization check that runs before the lock is ever taken.
     *
     * Given: the approval is in progress (lock held)
     * When:  another vastuuhenkilo, who has no right to approve it, sends an approval
     * Then:  the request is refused (not 200, and not the "already in progress" 409, which would
     *        mean it got as far as the lock), and the running approval completes as the rightful
     *        approver with one e-mail set.
     */
    @Test
    fun updateValmistumispyyntoByHyvaksyjaUserId_whenUnauthorizedPersonTriesDuringApproval_isRefusedAndDoesNotDisturbRunningApproval() {
        val valmistumispyyntoId = initTestInTransaction()
        val gate = gateFirstArchivingCall()
        val unauthorizedAuthentication = Saml2Authentication(testSamlPrincipal(anotherVastuuhenkilo.user!!.id, mapOf()), "test",
            listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        )

        val executor = Executors.newSingleThreadExecutor()
        try {
            val approval = executor.submit<Int> { putHyvaksynta(valmistumispyyntoId).response.status }
            assertThat(gate.firstEntered.await(30, TimeUnit.SECONDS)).withFailMessage("The approval never reached the archiving step").isTrue()

            val refused = putHyvaksynta(valmistumispyyntoId, sender = unauthorizedAuthentication)

            assertThat(refused.response.status).isNotEqualTo(200).isNotEqualTo(409)

            gate.release.countDown()
            assertThat(approval.get(30, TimeUnit.SECONDS)).isEqualTo(200)
        } finally {
            gate.release.countDown()
            executor.shutdownNow()
        }

        assertThat(gate.calls.get()).isEqualTo(1)
        verifyEmailSent()
        transactionTemplate.execute {
            val saved = em.find(
                fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java,
                valmistumispyyntoId
            )
            assertThat(saved.vastuuhenkiloHyvaksyja?.id).isEqualTo(vastuuhenkilo.id)
        }
    }

    /** Makes the FIRST archiving call block inside the approval transaction; later calls pass through. */
    private class ArchivingGate {
        val firstEntered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger(0)
    }

    private fun gateFirstArchivingCall(): ArchivingGate {
        val gate = ArchivingGate()
        whenever(arkistointiService.onKaytossa(any(), any())).thenReturn(true)
        whenever(arkistointiJobCreator.create(any())).thenAnswer {
            if (gate.calls.incrementAndGet() == 1) {
                gate.firstEntered.countDown()
                check(gate.release.await(30, TimeUnit.SECONDS)) {
                    "Test did not release the held approval in time"
                }
            }
            ArkistointiJob(id = UUID.randomUUID())
        }
        return gate
    }

    /** Every stored asiakirja of the opintooikeus is linked to its valmistumispyynto: one set, no orphans. */
    private fun assertOwnCompleteDocumentSet(valmistumispyyntoId: Long, opintooikeusId: Long) {
        transactionTemplate.execute {
            val saved = em.find(
                fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java,
                valmistumispyyntoId
            )
            assertThat(saved.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
            val linked = listOfNotNull(saved.yhteenvetoAsiakirja, saved.liitteetAsiakirja, saved.erikoistujanTiedotAsiakirja).size
            val stored = (em.createNativeQuery("SELECT COUNT(*) FROM asiakirja WHERE opintooikeus_id = $opintooikeusId").singleResult as Number).toInt()
            assertThat(linked).isGreaterThan(0)
            assertThat(stored).withFailMessage("Expected one document set ($linked documents) but found $stored").isEqualTo(linked)
        }
    }
}
