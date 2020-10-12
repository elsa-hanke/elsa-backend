package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpintooikeustiedotDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(OpintooikeustiedotDTO::class)
        val opintooikeustiedotDTO1 = OpintooikeustiedotDTO()
        opintooikeustiedotDTO1.id = 1L
        val opintooikeustiedotDTO2 = OpintooikeustiedotDTO()
        assertThat(opintooikeustiedotDTO1).isNotEqualTo(opintooikeustiedotDTO2)
        opintooikeustiedotDTO2.id = opintooikeustiedotDTO1.id
        assertThat(opintooikeustiedotDTO1).isEqualTo(opintooikeustiedotDTO2)
        opintooikeustiedotDTO2.id = 2L
        assertThat(opintooikeustiedotDTO1).isNotEqualTo(opintooikeustiedotDTO2)
        opintooikeustiedotDTO1.id = null
        assertThat(opintooikeustiedotDTO1).isNotEqualTo(opintooikeustiedotDTO2)
    }
}
