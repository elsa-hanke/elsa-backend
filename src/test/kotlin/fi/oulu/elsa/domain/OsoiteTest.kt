package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OsoiteTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Osoite::class)
        val osoite1 = Osoite()
        osoite1.id = 1L
        val osoite2 = Osoite()
        osoite2.id = osoite1.id
        assertThat(osoite1).isEqualTo(osoite2)
        osoite2.id = 2L
        assertThat(osoite1).isNotEqualTo(osoite2)
        osoite1.id = null
        assertThat(osoite1).isNotEqualTo(osoite2)
    }
}
