package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoejaksoTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Koejakso::class)
        val koejakso1 = Koejakso()
        koejakso1.id = 1L
        val koejakso2 = Koejakso()
        koejakso2.id = koejakso1.id
        assertThat(koejakso1).isEqualTo(koejakso2)
        koejakso2.id = 2L
        assertThat(koejakso1).isNotEqualTo(koejakso2)
        koejakso1.id = null
        assertThat(koejakso1).isNotEqualTo(koejakso2)
    }
}
