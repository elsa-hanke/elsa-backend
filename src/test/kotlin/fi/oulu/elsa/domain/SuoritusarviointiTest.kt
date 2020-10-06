package fi.oulu.elsa.domain

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritusarviointiTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Suoritusarviointi::class)
        val suoritusarviointi1 = Suoritusarviointi()
        suoritusarviointi1.id = 1L
        val suoritusarviointi2 = Suoritusarviointi()
        suoritusarviointi2.id = suoritusarviointi1.id
        assertThat(suoritusarviointi1).isEqualTo(suoritusarviointi2)
        suoritusarviointi2.id = 2L
        assertThat(suoritusarviointi1).isNotEqualTo(suoritusarviointi2)
        suoritusarviointi1.id = null
        assertThat(suoritusarviointi1).isNotEqualTo(suoritusarviointi2)
    }
}
