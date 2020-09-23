package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ErikoistuvaLaakariDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(ErikoistuvaLaakariDTO::class)
        val erikoistuvaLaakariDTO1 = ErikoistuvaLaakariDTO()
        erikoistuvaLaakariDTO1.id = 1L
        val erikoistuvaLaakariDTO2 = ErikoistuvaLaakariDTO()
        assertThat(erikoistuvaLaakariDTO1).isNotEqualTo(erikoistuvaLaakariDTO2)
        erikoistuvaLaakariDTO2.id = erikoistuvaLaakariDTO1.id
        assertThat(erikoistuvaLaakariDTO1).isEqualTo(erikoistuvaLaakariDTO2)
        erikoistuvaLaakariDTO2.id = 2L
        assertThat(erikoistuvaLaakariDTO1).isNotEqualTo(erikoistuvaLaakariDTO2)
        erikoistuvaLaakariDTO1.id = null
        assertThat(erikoistuvaLaakariDTO1).isNotEqualTo(erikoistuvaLaakariDTO2)
    }
}
