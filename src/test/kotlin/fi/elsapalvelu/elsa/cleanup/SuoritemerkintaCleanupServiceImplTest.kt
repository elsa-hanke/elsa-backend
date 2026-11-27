package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.suoritteet.Suoritemerkinta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import java.time.LocalDate

class SuoritemerkintaCleanupServiceImplTest {
    private val service = SuoritemerkintaCleanupServiceImpl()

    @Test
    fun `cleans every entry without changing the set or other fields`() {
        val entries = setOf(
            entry(1L, "First entry"),
            entry(2L, "Second entry"),
            entry(3L, null)
        )
        val before = entries.map { it.copy() }

        service.cleanSuoritemerkinnat(entries)

        assertThat(entries).hasSize(3)
        entries.zip(before).forEach { (entry, original) ->
            assertCleaned(entry, original)
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["Additional information"])
    fun `clears additional information without changing other fields`(additionalInformation: String?) {
        val entry = entry(1L, additionalInformation)
        val before = entry.copy()

        service.cleanSuoritemerkinta(entry)

        assertCleaned(entry, before)
    }

    @Test
    fun `accepts a null set`() {
        assertDoesNotThrow { service.cleanSuoritemerkinnat(null) }
    }

    @Test
    fun `accepts an empty set`() {
        assertDoesNotThrow { service.cleanSuoritemerkinnat(emptySet()) }
    }

    @Test
    fun `repeated cleanup leaves entries unchanged`() {
        val entry = entry(1L, "Additional information")
        val entries = setOf(entry)
        service.cleanSuoritemerkinnat(entries)
        val afterFirstCleanup = entry.copy()

        service.cleanSuoritemerkinnat(entries)

        assertCleaned(entry, afterFirstCleanup)
    }

    private fun entry(id: Long, additionalInformation: String?) = Suoritemerkinta(
        id = id,
        suorituspaiva = LocalDate.of(2020, 1, 2),
        arviointiasteikonTaso = 3,
        vaativuustaso = 4,
        lisatiedot = additionalInformation,
        lukittu = true,
        suorite = mock(),
        tyoskentelyjakso = mock(),
        arviointiasteikko = mock()
    )

    private fun assertCleaned(entry: Suoritemerkinta, original: Suoritemerkinta) {
        assertThat(entry.lisatiedot).isEqualTo("")
        assertThat(entry).usingRecursiveComparison()
            .ignoringFields("lisatiedot")
            .isEqualTo(original)
        assertThat(entry.suorite).isSameAs(original.suorite)
        assertThat(entry.tyoskentelyjakso).isSameAs(original.tyoskentelyjakso)
        assertThat(entry.arviointiasteikko).isSameAs(original.arviointiasteikko)
    }
}
