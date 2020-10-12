package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PikaviestiKeskusteluTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(PikaviestiKeskustelu::class)
        val pikaviestiKeskustelu1 = PikaviestiKeskustelu()
        pikaviestiKeskustelu1.id = 1L
        val pikaviestiKeskustelu2 = PikaviestiKeskustelu()
        pikaviestiKeskustelu2.id = pikaviestiKeskustelu1.id
        assertThat(pikaviestiKeskustelu1).isEqualTo(pikaviestiKeskustelu2)
        pikaviestiKeskustelu2.id = 2L
        assertThat(pikaviestiKeskustelu1).isNotEqualTo(pikaviestiKeskustelu2)
        pikaviestiKeskustelu1.id = null
        assertThat(pikaviestiKeskustelu1).isNotEqualTo(pikaviestiKeskustelu2)
    }
}
