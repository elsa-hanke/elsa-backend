package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaivakirjamerkintaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Paivakirjamerkinta::class)
        val paivakirjamerkinta1 = Paivakirjamerkinta()
        paivakirjamerkinta1.id = 1L
        val paivakirjamerkinta2 = Paivakirjamerkinta()
        paivakirjamerkinta2.id = paivakirjamerkinta1.id
        assertThat(paivakirjamerkinta1).isEqualTo(paivakirjamerkinta2)
        paivakirjamerkinta2.id = 2L
        assertThat(paivakirjamerkinta1).isNotEqualTo(paivakirjamerkinta2)
        paivakirjamerkinta1.id = null
        assertThat(paivakirjamerkinta1).isNotEqualTo(paivakirjamerkinta2)
    }
}
