package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

class OpintooikeusCleanupOrchestratorTest {
    private val repository = mock<OpintooikeusRepository>()
    private val cleanupService = mock<OpintooikeusCleanupService>()
    private val doctorCleanupService = mock<ErikoistuvaLaakariCleanupService>()
    private val orchestrator = OpintooikeusCleanupOrchestrator(
        cleanupService,
        repository,
        mock(),
        mock(),
        mock(),
        mock(),
        doctorCleanupService
    )

    @Test
    fun `skips missing or not yet eligible viewing dates`() {
        val today = LocalDate.now()
        val dates = listOf(
            null,
            today.minusMonths(1),
            today.minusDays(1),
            today,
            today.plusDays(1),
            LocalDate.of(2123, 7, 1)
        )
        dates.forEachIndexed { index, date ->
            val id = index.toLong() + 1
            whenever(repository.findById(id)).thenReturn(Optional.of(Opintooikeus(id = id, viimeinenKatselupaiva = date)))

            orchestrator.clearContent(id)
        }

        verifyNoInteractions(cleanupService, doctorCleanupService)
    }

    @Test
    fun `starts cleanup when viewing period ended more than one month ago`() {
        val right = Opintooikeus(id = 1L, viimeinenKatselupaiva = LocalDate.now().minusMonths(1).minusDays(1))
        whenever(repository.findById(1L)).thenReturn(Optional.of(right))

        val exception = assertThrows<Exception> { orchestrator.clearContent(1L) }

        verify(cleanupService).clearOpintooikeus(right)
        assertThat(exception.message).contains("Cleanup not fully implemented")
    }
}
