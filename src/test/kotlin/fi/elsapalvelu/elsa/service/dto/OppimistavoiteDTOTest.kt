package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppimistavoiteDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(OppimistavoiteDTO::class)
        val oppimistavoiteDTO1 = OppimistavoiteDTO()
        oppimistavoiteDTO1.id = 1L
        val oppimistavoiteDTO2 = OppimistavoiteDTO()
        assertThat(oppimistavoiteDTO1).isNotEqualTo(oppimistavoiteDTO2)
        oppimistavoiteDTO2.id = oppimistavoiteDTO1.id
        assertThat(oppimistavoiteDTO1).isEqualTo(oppimistavoiteDTO2)
        oppimistavoiteDTO2.id = 2L
        assertThat(oppimistavoiteDTO1).isNotEqualTo(oppimistavoiteDTO2)
        oppimistavoiteDTO1.id = null
        assertThat(oppimistavoiteDTO1).isNotEqualTo(oppimistavoiteDTO2)
    }
}
