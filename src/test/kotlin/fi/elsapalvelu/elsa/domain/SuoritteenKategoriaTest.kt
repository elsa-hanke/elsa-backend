package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritteenKategoriaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(SuoritteenKategoria::class)
        val suoritteenKategoria1 = SuoritteenKategoria()
        suoritteenKategoria1.id = 1L
        val suoritteenKategoria2 = SuoritteenKategoria()
        suoritteenKategoria2.id = suoritteenKategoria1.id
        assertThat(suoritteenKategoria1).isEqualTo(suoritteenKategoria2)
        suoritteenKategoria2.id = 2L
        assertThat(suoritteenKategoria1).isNotEqualTo(suoritteenKategoria2)
        suoritteenKategoria1.id = null
        assertThat(suoritteenKategoria1).isNotEqualTo(suoritteenKategoria2)
    }
}
