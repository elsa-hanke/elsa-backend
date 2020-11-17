package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritemerkintaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(SuoritemerkintaDTO::class)
        val suoritemerkintaDTO1 = SuoritemerkintaDTO()
        suoritemerkintaDTO1.id = 1L
        val suoritemerkintaDTO2 = SuoritemerkintaDTO()
        assertThat(suoritemerkintaDTO1).isNotEqualTo(suoritemerkintaDTO2)
        suoritemerkintaDTO2.id = suoritemerkintaDTO1.id
        assertThat(suoritemerkintaDTO1).isEqualTo(suoritemerkintaDTO2)
        suoritemerkintaDTO2.id = 2L
        assertThat(suoritemerkintaDTO1).isNotEqualTo(suoritemerkintaDTO2)
        suoritemerkintaDTO1.id = null
        assertThat(suoritemerkintaDTO1).isNotEqualTo(suoritemerkintaDTO2)
    }
}
