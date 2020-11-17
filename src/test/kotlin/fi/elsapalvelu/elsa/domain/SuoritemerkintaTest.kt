package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritemerkintaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Suoritemerkinta::class)
        val suoritemerkinta1 = Suoritemerkinta()
        suoritemerkinta1.id = 1L
        val suoritemerkinta2 = Suoritemerkinta()
        suoritemerkinta2.id = suoritemerkinta1.id
        assertThat(suoritemerkinta1).isEqualTo(suoritemerkinta2)
        suoritemerkinta2.id = 2L
        assertThat(suoritemerkinta1).isNotEqualTo(suoritemerkinta2)
        suoritemerkinta1.id = null
        assertThat(suoritemerkinta1).isNotEqualTo(suoritemerkinta2)
    }
}
