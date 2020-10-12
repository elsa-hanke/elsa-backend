package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HopsTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Hops::class)
        val hops1 = Hops()
        hops1.id = 1L
        val hops2 = Hops()
        hops2.id = hops1.id
        assertThat(hops1).isEqualTo(hops2)
        hops2.id = 2L
        assertThat(hops1).isNotEqualTo(hops2)
        hops1.id = null
        assertThat(hops1).isNotEqualTo(hops2)
    }
}
