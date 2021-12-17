package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoriteDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(SuoriteDTO::class)
        val suoriteDTO1 = SuoriteDTO()
        suoriteDTO1.id = 1L
        val suoriteDTO2 = SuoriteDTO()
        assertThat(suoriteDTO1).isNotEqualTo(suoriteDTO2)
        suoriteDTO2.id = suoriteDTO1.id
        assertThat(suoriteDTO1).isEqualTo(suoriteDTO2)
        suoriteDTO2.id = 2L
        assertThat(suoriteDTO1).isNotEqualTo(suoriteDTO2)
        suoriteDTO1.id = null
        assertThat(suoriteDTO1).isNotEqualTo(suoriteDTO2)
    }
}
