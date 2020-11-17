package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppimistavoitteenKategoriaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(OppimistavoitteenKategoria::class)
        val oppimistavoitteenKategoria1 = OppimistavoitteenKategoria()
        oppimistavoitteenKategoria1.id = 1L
        val oppimistavoitteenKategoria2 = OppimistavoitteenKategoria()
        oppimistavoitteenKategoria2.id = oppimistavoitteenKategoria1.id
        assertThat(oppimistavoitteenKategoria1).isEqualTo(oppimistavoitteenKategoria2)
        oppimistavoitteenKategoria2.id = 2L
        assertThat(oppimistavoitteenKategoria1).isNotEqualTo(oppimistavoitteenKategoria2)
        oppimistavoitteenKategoria1.id = null
        assertThat(oppimistavoitteenKategoria1).isNotEqualTo(oppimistavoitteenKategoria2)
    }
}
