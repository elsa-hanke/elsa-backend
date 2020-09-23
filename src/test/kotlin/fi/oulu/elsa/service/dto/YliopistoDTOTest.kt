package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YliopistoDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(YliopistoDTO::class)
        val yliopistoDTO1 = YliopistoDTO()
        yliopistoDTO1.id = 1L
        val yliopistoDTO2 = YliopistoDTO()
        assertThat(yliopistoDTO1).isNotEqualTo(yliopistoDTO2)
        yliopistoDTO2.id = yliopistoDTO1.id
        assertThat(yliopistoDTO1).isEqualTo(yliopistoDTO2)
        yliopistoDTO2.id = 2L
        assertThat(yliopistoDTO1).isNotEqualTo(yliopistoDTO2)
        yliopistoDTO1.id = null
        assertThat(yliopistoDTO1).isNotEqualTo(yliopistoDTO2)
    }
}
