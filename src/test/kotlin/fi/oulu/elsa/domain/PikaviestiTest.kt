package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PikaviestiTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Pikaviesti::class)
        val pikaviesti1 = Pikaviesti()
        pikaviesti1.id = 1L
        val pikaviesti2 = Pikaviesti()
        pikaviesti2.id = pikaviesti1.id
        assertThat(pikaviesti1).isEqualTo(pikaviesti2)
        pikaviesti2.id = 2L
        assertThat(pikaviesti1).isNotEqualTo(pikaviesti2)
        pikaviesti1.id = null
        assertThat(pikaviesti1).isNotEqualTo(pikaviesti2)
    }
}
