package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppimistavoiteTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Oppimistavoite::class)
        val oppimistavoite1 = Oppimistavoite()
        oppimistavoite1.id = 1L
        val oppimistavoite2 = Oppimistavoite()
        oppimistavoite2.id = oppimistavoite1.id
        assertThat(oppimistavoite1).isEqualTo(oppimistavoite2)
        oppimistavoite2.id = 2L
        assertThat(oppimistavoite1).isNotEqualTo(oppimistavoite2)
        oppimistavoite1.id = null
        assertThat(oppimistavoite1).isNotEqualTo(oppimistavoite2)
    }
}
