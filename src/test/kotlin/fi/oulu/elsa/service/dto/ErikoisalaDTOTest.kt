package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ErikoisalaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(ErikoisalaDTO::class)
        val erikoisalaDTO1 = ErikoisalaDTO()
        erikoisalaDTO1.id = 1L
        val erikoisalaDTO2 = ErikoisalaDTO()
        assertThat(erikoisalaDTO1).isNotEqualTo(erikoisalaDTO2)
        erikoisalaDTO2.id = erikoisalaDTO1.id
        assertThat(erikoisalaDTO1).isEqualTo(erikoisalaDTO2)
        erikoisalaDTO2.id = 2L
        assertThat(erikoisalaDTO1).isNotEqualTo(erikoisalaDTO2)
        erikoisalaDTO1.id = null
        assertThat(erikoisalaDTO1).isNotEqualTo(erikoisalaDTO2)
    }
}
