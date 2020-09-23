package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KayttajaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Kayttaja::class)
        val kayttaja1 = Kayttaja()
        kayttaja1.id = 1L
        val kayttaja2 = Kayttaja()
        kayttaja2.id = kayttaja1.id
        assertThat(kayttaja1).isEqualTo(kayttaja2)
        kayttaja2.id = 2L
        assertThat(kayttaja1).isNotEqualTo(kayttaja2)
        kayttaja1.id = null
        assertThat(kayttaja1).isNotEqualTo(kayttaja2)
    }
}
