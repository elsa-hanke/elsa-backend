package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SuoritteenKategoriaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(SuoritteenKategoriaDTO::class)
        val suoritteenKategoriaDTO1 = SuoritteenKategoriaDTO()
        suoritteenKategoriaDTO1.id = 1L
        val suoritteenKategoriaDTO2 = SuoritteenKategoriaDTO()
        assertThat(suoritteenKategoriaDTO1).isNotEqualTo(suoritteenKategoriaDTO2)
        suoritteenKategoriaDTO2.id = suoritteenKategoriaDTO1.id
        assertThat(suoritteenKategoriaDTO1).isEqualTo(suoritteenKategoriaDTO2)
        suoritteenKategoriaDTO2.id = 2L
        assertThat(suoritteenKategoriaDTO1).isNotEqualTo(suoritteenKategoriaDTO2)
        suoritteenKategoriaDTO1.id = null
        assertThat(suoritteenKategoriaDTO1).isNotEqualTo(suoritteenKategoriaDTO2)
    }
}
