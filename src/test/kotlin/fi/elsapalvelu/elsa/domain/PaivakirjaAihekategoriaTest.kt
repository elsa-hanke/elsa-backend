package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaivakirjaAihekategoriaTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(PaivakirjaAihekategoria::class)
        val paivakirjaAihekategoria1 = PaivakirjaAihekategoria()
        paivakirjaAihekategoria1.id = 1L
        val paivakirjaAihekategoria2 = PaivakirjaAihekategoria()
        paivakirjaAihekategoria2.id = paivakirjaAihekategoria1.id
        assertThat(paivakirjaAihekategoria1).isEqualTo(paivakirjaAihekategoria2)
        paivakirjaAihekategoria2.id = 2L
        assertThat(paivakirjaAihekategoria1).isNotEqualTo(paivakirjaAihekategoria2)
        paivakirjaAihekategoria1.id = null
        assertThat(paivakirjaAihekategoria1).isNotEqualTo(paivakirjaAihekategoria2)
    }
}
