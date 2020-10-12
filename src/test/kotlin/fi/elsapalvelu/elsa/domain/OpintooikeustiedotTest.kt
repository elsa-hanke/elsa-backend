package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpintooikeustiedotTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Opintooikeustiedot::class)
        val opintooikeustiedot1 = Opintooikeustiedot()
        opintooikeustiedot1.id = 1L
        val opintooikeustiedot2 = Opintooikeustiedot()
        opintooikeustiedot2.id = opintooikeustiedot1.id
        assertThat(opintooikeustiedot1).isEqualTo(opintooikeustiedot2)
        opintooikeustiedot2.id = 2L
        assertThat(opintooikeustiedot1).isNotEqualTo(opintooikeustiedot2)
        opintooikeustiedot1.id = null
        assertThat(opintooikeustiedot1).isNotEqualTo(opintooikeustiedot2)
    }
}
