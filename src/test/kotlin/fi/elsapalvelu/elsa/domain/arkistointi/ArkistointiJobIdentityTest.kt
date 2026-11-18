package fi.elsapalvelu.elsa.domain.arkistointi

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArkistointiJobIdentityTest {
    @Test
    fun `unsaved jobs do not share an identity even with the same business key`() {
        val first = ArkistointiJob(key = "valmistuminen:TAMPEREEN_YLIOPISTO:123")
        val second = ArkistointiJob(key = first.key)

        assertThat(first.id).isNull()
        assertThat(second.id).isNull()
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `copying the same job retains its delivery identity`() {
        val original = ArkistointiJob(id = UUID.randomUUID(), key = "business-key")
        val updated = original.copy(status = ArkistointiJobStatus.KASITTELYSSA)

        assertThat(updated.id).isEqualTo(original.id)
        assertThat(updated.key).isEqualTo(original.key)
    }
}
