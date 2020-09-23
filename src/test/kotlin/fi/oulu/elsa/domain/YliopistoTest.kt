package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YliopistoTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Yliopisto::class)
        val yliopisto1 = Yliopisto()
        yliopisto1.id = 1L
        val yliopisto2 = Yliopisto()
        yliopisto2.id = yliopisto1.id
        assertThat(yliopisto1).isEqualTo(yliopisto2)
        yliopisto2.id = 2L
        assertThat(yliopisto1).isNotEqualTo(yliopisto2)
        yliopisto1.id = null
        assertThat(yliopisto1).isNotEqualTo(yliopisto2)
    }
}
