package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.databind.JsonMappingException
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import java.io.IOException
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.isA
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Every failure inside the approval transaction must roll back the approval, documents and scheduler row. */
class KoejaksoApprovalRollbackIT : KoejaksoArchiveItBase() {

    @ForEachKoejaksoCaseType
    fun `checked payload serialization failure rolls back asynchronous approval and real PDF`(type: CaseType) {
        val fixture = fixture()
        doAnswer { throw JsonMappingException.fromUnexpectedIOE(IOException("Simulated serialization failure")) }
            .whenever(archiveMapper).writeValueAsString(isA<KoejaksoArkistointiPayload>())

        approve(fixture, type).andExpect(status().isInternalServerError)

        assertRolledBack(fixture, type)
        verify(snapshots).createReference(any(), any(), any())
        verifyNoInteractions(creator, scheduler)
    }

    @ForEachKoejaksoCaseType
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

    @ForEachKoejaksoCaseType
    fun `scheduler failure rolls back approval documents and notifications`(type: CaseType) {
        val fixture = fixture()
        transactions.executeWithoutResult {
            doReturn(false).whenever(scheduler).schedule(any(), any())
        }
        approve(fixture, type).andExpect(status().isInternalServerError)
        assertRolledBack(fixture, type)
    }

    @ForEachKoejaksoCaseType
    fun `snapshot failure is safely reported and rolls back`(type: CaseType) {
        val fixture = fixture()
        doThrow(ArkistointiAsiakirjaException(ArkistointiError.create("ARCHIVE_DOCUMENT_INVALID", "unsafe.pdf")))
            .whenever(snapshots).createReference(any(), any(), any())
        approve(fixture, type).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.errorKey").value("ARCHIVE_DOCUMENT_INVALID"))
            .andExpect(jsonPath("$.detail").value("Arkistoitavien asiakirjojen kasittely epaonnistui."))
        assertRolledBack(fixture, type)
    }

    @ForEachKoejaksoCaseType
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

    @ForEachKoejaksoCaseType
    fun `creator failure rolls back approval and snapshot`(type: CaseType) {
        val fixture = fixture()
        doThrow(IllegalStateException("test failure")).whenever(creator).create(any())
        approve(fixture, type).andExpect(status().isInternalServerError)
        assertRolledBack(fixture, type)
    }

    @ForEachKoejaksoCaseType
    fun `missing previous approval prevents final decision and creates no documents`(type: CaseType) {
        val fixture = fixture()
        withdrawPreviousApproval(fixture, type)
        approve(fixture, type).andExpect(status().isBadRequest)
        assertRolledBack(fixture, type)
    }
}
