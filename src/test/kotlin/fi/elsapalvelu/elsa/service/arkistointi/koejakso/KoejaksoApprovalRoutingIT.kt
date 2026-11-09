package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Sync vs async routing: per-case-type opt-in and the unchanged legacy (sync) behaviour. */
class KoejaksoApprovalRoutingIT : KoejaksoArchiveItBase() {

    @ForEachKoejaksoCaseType
    fun `case type opt in is independent and sync creates no archive job`(type: CaseType) {
        setAsyncEnabled(type, false)
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        verifyLegacyDelivery(type)
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(1)
        val otherType = if (type == CaseType.SOPIMUS) CaseType.KOEJAKSO else CaseType.SOPIMUS
        approve(fixture, otherType).andExpect(status().isOk)
        assertThat(job(fixture, otherType).id).isNotNull()
    }

    @ForEachKoejaksoCaseType
    fun `sync service preserves approval without the new previous approval checks`(type: CaseType) {
        setAsyncEnabled(type, false)
        val fixture = fixture()
        withdrawPreviousApproval(fixture, type)

        // Existing HTTP guards still apply; exercise the service boundary changed by task5 directly.
        updateApproval(fixture, type)

        verifyLegacyDelivery(type)
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(1)
        verifyNoAsyncInteractions()
    }

    @ForEachKoejaksoCaseType
    fun `sync service preserves repeated approval without the new async guard`(type: CaseType) {
        setAsyncEnabled(type, false)
        val fixture = fixture()

        updateApproval(fixture, type)
        updateApproval(fixture, type)

        verifyLegacyDelivery(type, times = 2)
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(2)
        verifyNoAsyncInteractions()
    }

    @Test
    fun `sync HTTP still rejects assessment reapproval as on main`() {
        setAsyncEnabled(CaseType.KOEJAKSO, false)
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
        setAsyncEnabled(CaseType.SOPIMUS, false)
        val fixture = fixture()
        withdrawPreviousApproval(fixture, CaseType.SOPIMUS)

        approve(fixture, CaseType.SOPIMUS).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(
                "error.dataillegal.vastuuhenkilo-ei-saa-muokata-sopimusta-jos-kouluttajat-eivat-ole-hyvaksyneet-sita"
            ))

        assertAwaitingFinalApproval(fixture, CaseType.SOPIMUS)
    }

    @ForEachKoejaksoCaseType
    fun `disabled archiving still generates ordinary PDF without a snapshot`(type: CaseType) {
        val fixture = fixture()
        properties.getArkistointi().getTre().kaytossa = false
        approve(fixture, type).andExpect(status().isOk)
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(1)
    }

    private fun verifyLegacyDelivery(type: CaseType, times: Int = 1) {
        verify(legacy, times(times)).laheta(any(), eq("/tmp/koejakso-test.zip"), eq(type), any(), anyOrNull(), anyOrNull())
    }

    private fun verifyNoAsyncInteractions() {
        verifyNoInteractions(snapshots, creator, scheduler)
        verify(koulutussopimusRepository, never()).findByIdForUpdate(any())
        verify(vastuuhenkilonArvioRepository, never()).findByIdForUpdate(any())
    }
}
