package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HopsDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(HopsDTO::class)
        val hopsDTO1 = HopsDTO()
        hopsDTO1.id = 1L
        val hopsDTO2 = HopsDTO()
        assertThat(hopsDTO1).isNotEqualTo(hopsDTO2)
        hopsDTO2.id = hopsDTO1.id
        assertThat(hopsDTO1).isEqualTo(hopsDTO2)
        hopsDTO2.id = 2L
        assertThat(hopsDTO1).isNotEqualTo(hopsDTO2)
        hopsDTO1.id = null
        assertThat(hopsDTO1).isNotEqualTo(hopsDTO2)
    }
}
