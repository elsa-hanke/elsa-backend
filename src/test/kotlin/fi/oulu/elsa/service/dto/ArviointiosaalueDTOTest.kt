package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArviointiosaalueDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(ArviointiosaalueDTO::class)
        val arviointiosaalueDTO1 = ArviointiosaalueDTO()
        arviointiosaalueDTO1.id = 1L
        val arviointiosaalueDTO2 = ArviointiosaalueDTO()
        assertThat(arviointiosaalueDTO1).isNotEqualTo(arviointiosaalueDTO2)
        arviointiosaalueDTO2.id = arviointiosaalueDTO1.id
        assertThat(arviointiosaalueDTO1).isEqualTo(arviointiosaalueDTO2)
        arviointiosaalueDTO2.id = 2L
        assertThat(arviointiosaalueDTO1).isNotEqualTo(arviointiosaalueDTO2)
        arviointiosaalueDTO1.id = null
        assertThat(arviointiosaalueDTO1).isNotEqualTo(arviointiosaalueDTO2)
    }
}
