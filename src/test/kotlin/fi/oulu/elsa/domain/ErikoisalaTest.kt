package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ErikoisalaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Erikoisala::class)
        val erikoisala1 = Erikoisala()
        erikoisala1.id = 1L
        val erikoisala2 = Erikoisala()
        erikoisala2.id = erikoisala1.id
        assertThat(erikoisala1).isEqualTo(erikoisala2)
        erikoisala2.id = 2L
        assertThat(erikoisala1).isNotEqualTo(erikoisala2)
        erikoisala1.id = null
        assertThat(erikoisala1).isNotEqualTo(erikoisala2)
    }
}
