package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ErikoistuvaLaakariTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(ErikoistuvaLaakari::class)
        val erikoistuvaLaakari1 = ErikoistuvaLaakari()
        erikoistuvaLaakari1.id = 1L
        val erikoistuvaLaakari2 = ErikoistuvaLaakari()
        erikoistuvaLaakari2.id = erikoistuvaLaakari1.id
        assertThat(erikoistuvaLaakari1).isEqualTo(erikoistuvaLaakari2)
        erikoistuvaLaakari2.id = 2L
        assertThat(erikoistuvaLaakari1).isNotEqualTo(erikoistuvaLaakari2)
        erikoistuvaLaakari1.id = null
        assertThat(erikoistuvaLaakari1).isNotEqualTo(erikoistuvaLaakari2)
    }
}
