package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaivakirjaAihekategoriaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(PaivakirjaAihekategoriaDTO::class)
        val paivakirjaAihekategoriaDTO1 = PaivakirjaAihekategoriaDTO()
        paivakirjaAihekategoriaDTO1.id = 1L
        val paivakirjaAihekategoriaDTO2 = PaivakirjaAihekategoriaDTO()
        assertThat(paivakirjaAihekategoriaDTO1).isNotEqualTo(paivakirjaAihekategoriaDTO2)
        paivakirjaAihekategoriaDTO2.id = paivakirjaAihekategoriaDTO1.id
        assertThat(paivakirjaAihekategoriaDTO1).isEqualTo(paivakirjaAihekategoriaDTO2)
        paivakirjaAihekategoriaDTO2.id = 2L
        assertThat(paivakirjaAihekategoriaDTO1).isNotEqualTo(paivakirjaAihekategoriaDTO2)
        paivakirjaAihekategoriaDTO1.id = null
        assertThat(paivakirjaAihekategoriaDTO1).isNotEqualTo(paivakirjaAihekategoriaDTO2)
    }
}
