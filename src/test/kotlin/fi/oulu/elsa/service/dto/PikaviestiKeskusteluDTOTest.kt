package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PikaviestiKeskusteluDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(PikaviestiKeskusteluDTO::class)
        val pikaviestiKeskusteluDTO1 = PikaviestiKeskusteluDTO()
        pikaviestiKeskusteluDTO1.id = 1L
        val pikaviestiKeskusteluDTO2 = PikaviestiKeskusteluDTO()
        assertThat(pikaviestiKeskusteluDTO1).isNotEqualTo(pikaviestiKeskusteluDTO2)
        pikaviestiKeskusteluDTO2.id = pikaviestiKeskusteluDTO1.id
        assertThat(pikaviestiKeskusteluDTO1).isEqualTo(pikaviestiKeskusteluDTO2)
        pikaviestiKeskusteluDTO2.id = 2L
        assertThat(pikaviestiKeskusteluDTO1).isNotEqualTo(pikaviestiKeskusteluDTO2)
        pikaviestiKeskusteluDTO1.id = null
        assertThat(pikaviestiKeskusteluDTO1).isNotEqualTo(pikaviestiKeskusteluDTO2)
    }
}
