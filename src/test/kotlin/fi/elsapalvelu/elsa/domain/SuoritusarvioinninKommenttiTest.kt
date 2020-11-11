package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritusarvioinninKommenttiTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(SuoritusarvioinninKommentti::class)
        val suoritusarvioinninKommentti1 = SuoritusarvioinninKommentti()
        suoritusarvioinninKommentti1.id = 1L
        val suoritusarvioinninKommentti2 = SuoritusarvioinninKommentti()
        suoritusarvioinninKommentti2.id = suoritusarvioinninKommentti1.id
        assertThat(suoritusarvioinninKommentti1).isEqualTo(suoritusarvioinninKommentti2)
        suoritusarvioinninKommentti2.id = 2L
        assertThat(suoritusarvioinninKommentti1).isNotEqualTo(suoritusarvioinninKommentti2)
        suoritusarvioinninKommentti1.id = null
        assertThat(suoritusarvioinninKommentti1).isNotEqualTo(suoritusarvioinninKommentti2)
    }
}
