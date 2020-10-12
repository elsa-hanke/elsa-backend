package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritusarviointiDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(SuoritusarviointiDTO::class)
        val suoritusarviointiDTO1 = SuoritusarviointiDTO()
        suoritusarviointiDTO1.id = 1L
        val suoritusarviointiDTO2 = SuoritusarviointiDTO()
        assertThat(suoritusarviointiDTO1).isNotEqualTo(suoritusarviointiDTO2)
        suoritusarviointiDTO2.id = suoritusarviointiDTO1.id
        assertThat(suoritusarviointiDTO1).isEqualTo(suoritusarviointiDTO2)
        suoritusarviointiDTO2.id = 2L
        assertThat(suoritusarviointiDTO1).isNotEqualTo(suoritusarviointiDTO2)
        suoritusarviointiDTO1.id = null
        assertThat(suoritusarviointiDTO1).isNotEqualTo(suoritusarviointiDTO2)
    }
}
