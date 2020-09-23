package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArviointiosaalueTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Arviointiosaalue::class)
        val arviointiosaalue1 = Arviointiosaalue()
        arviointiosaalue1.id = 1L
        val arviointiosaalue2 = Arviointiosaalue()
        arviointiosaalue2.id = arviointiosaalue1.id
        assertThat(arviointiosaalue1).isEqualTo(arviointiosaalue2)
        arviointiosaalue2.id = 2L
        assertThat(arviointiosaalue1).isNotEqualTo(arviointiosaalue2)
        arviointiosaalue1.id = null
        assertThat(arviointiosaalue1).isNotEqualTo(arviointiosaalue2)
    }
}
