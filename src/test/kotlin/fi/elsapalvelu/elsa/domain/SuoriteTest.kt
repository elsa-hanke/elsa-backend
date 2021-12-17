package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoriteTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Suorite::class)
        val suorite1 = Suorite()
        suorite1.id = 1L
        val suorite2 = Suorite()
        suorite2.id = suorite1.id
        assertThat(suorite1).isEqualTo(suorite2)
        suorite2.id = 2L
        assertThat(suorite1).isNotEqualTo(suorite2)
        suorite1.id = null
        assertThat(suorite1).isNotEqualTo(suorite2)
    }
}
