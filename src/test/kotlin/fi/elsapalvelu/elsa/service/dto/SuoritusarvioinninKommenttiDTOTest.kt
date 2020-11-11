package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritusarvioinninKommenttiDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(SuoritusarvioinninKommenttiDTO::class)
        val suoritusarvioinninKommenttiDTO1 = SuoritusarvioinninKommenttiDTO()
        suoritusarvioinninKommenttiDTO1.id = 1L
        val suoritusarvioinninKommenttiDTO2 = SuoritusarvioinninKommenttiDTO()
        assertThat(suoritusarvioinninKommenttiDTO1).isNotEqualTo(suoritusarvioinninKommenttiDTO2)
        suoritusarvioinninKommenttiDTO2.id = suoritusarvioinninKommenttiDTO1.id
        assertThat(suoritusarvioinninKommenttiDTO1).isEqualTo(suoritusarvioinninKommenttiDTO2)
        suoritusarvioinninKommenttiDTO2.id = 2L
        assertThat(suoritusarvioinninKommenttiDTO1).isNotEqualTo(suoritusarvioinninKommenttiDTO2)
        suoritusarvioinninKommenttiDTO1.id = null
        assertThat(suoritusarvioinninKommenttiDTO1).isNotEqualTo(suoritusarvioinninKommenttiDTO2)
    }
}
