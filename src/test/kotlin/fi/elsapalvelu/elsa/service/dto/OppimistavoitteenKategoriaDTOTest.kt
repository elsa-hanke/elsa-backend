package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppimistavoitteenKategoriaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(OppimistavoitteenKategoriaDTO::class)
        val oppimistavoitteenKategoriaDTO1 = OppimistavoitteenKategoriaDTO()
        oppimistavoitteenKategoriaDTO1.id = 1L
        val oppimistavoitteenKategoriaDTO2 = OppimistavoitteenKategoriaDTO()
        assertThat(oppimistavoitteenKategoriaDTO1).isNotEqualTo(oppimistavoitteenKategoriaDTO2)
        oppimistavoitteenKategoriaDTO2.id = oppimistavoitteenKategoriaDTO1.id
        assertThat(oppimistavoitteenKategoriaDTO1).isEqualTo(oppimistavoitteenKategoriaDTO2)
        oppimistavoitteenKategoriaDTO2.id = 2L
        assertThat(oppimistavoitteenKategoriaDTO1).isNotEqualTo(oppimistavoitteenKategoriaDTO2)
        oppimistavoitteenKategoriaDTO1.id = null
        assertThat(oppimistavoitteenKategoriaDTO1).isNotEqualTo(oppimistavoitteenKategoriaDTO2)
    }
}
