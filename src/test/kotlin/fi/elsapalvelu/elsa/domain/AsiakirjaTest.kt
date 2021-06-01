package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AsiakirjaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Asiakirja::class)
        val asiakirja1 = Asiakirja()
        asiakirja1.id = 1L
        val asiakirja2 = Asiakirja()
        asiakirja2.id = asiakirja1.id
        assertThat(asiakirja1).isEqualTo(asiakirja2)
        asiakirja2.id = 2L
        assertThat(asiakirja1).isNotEqualTo(asiakirja2)
        asiakirja1.id = null
        assertThat(asiakirja1).isNotEqualTo(asiakirja2)
    }
}
