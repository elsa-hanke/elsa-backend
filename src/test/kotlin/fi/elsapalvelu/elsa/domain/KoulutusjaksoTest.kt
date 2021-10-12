package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoulutusjaksoTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Koulutusjakso::class)
        val koulutusjakso1 = Koulutusjakso()
        koulutusjakso1.id = 1L
        val koulutusjakso2 = Koulutusjakso()
        koulutusjakso2.id = koulutusjakso1.id
        assertThat(koulutusjakso1).isEqualTo(koulutusjakso2)
        koulutusjakso2.id = 2L
        assertThat(koulutusjakso1).isNotEqualTo(koulutusjakso2)
        koulutusjakso1.id = null
        assertThat(koulutusjakso1).isNotEqualTo(koulutusjakso2)
    }
}
