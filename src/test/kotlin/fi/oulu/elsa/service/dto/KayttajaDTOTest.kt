package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KayttajaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KayttajaDTO::class)
        val kayttajaDTO1 = KayttajaDTO()
        kayttajaDTO1.id = 1L
        val kayttajaDTO2 = KayttajaDTO()
        assertThat(kayttajaDTO1).isNotEqualTo(kayttajaDTO2)
        kayttajaDTO2.id = kayttajaDTO1.id
        assertThat(kayttajaDTO1).isEqualTo(kayttajaDTO2)
        kayttajaDTO2.id = 2L
        assertThat(kayttajaDTO1).isNotEqualTo(kayttajaDTO2)
        kayttajaDTO1.id = null
        assertThat(kayttajaDTO1).isNotEqualTo(kayttajaDTO2)
    }
}
