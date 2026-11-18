package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DynastyStepKeyTest {
    @Test
    fun `keys preserve persisted ledger and operator script format`() {
        assertThat(listOf(DynastyStepKey.Case, DynastyStepKey.Task, DynastyStepKey.ArchiveCase,
            DynastyStepKey.Document(0), DynastyStepKey.Document(Int.MAX_VALUE)).map { it.databaseKey })
            .containsExactly("CASE", "TASK", "ARCHIVE_CASE", "DOCUMENT:0", "DOCUMENT:2147483647")
    }

    @Test
    fun `negative document order is rejected before accessing ledger`() {
        assertThatThrownBy { DynastyStepKey.Document(-1) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
