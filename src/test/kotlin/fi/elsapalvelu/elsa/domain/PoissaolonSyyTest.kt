package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PoissaolonSyyTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(PoissaolonSyy::class)
        val poissaolonSyy1 = PoissaolonSyy()
        poissaolonSyy1.id = 1L
        val poissaolonSyy2 = PoissaolonSyy()
        poissaolonSyy2.id = poissaolonSyy1.id
        assertThat(poissaolonSyy1).isEqualTo(poissaolonSyy2)
        poissaolonSyy2.id = 2L
        assertThat(poissaolonSyy1).isNotEqualTo(poissaolonSyy2)
        poissaolonSyy1.id = null
        assertThat(poissaolonSyy1).isNotEqualTo(poissaolonSyy2)
    }
}
