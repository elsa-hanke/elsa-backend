package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoejaksoDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KoejaksoDTO::class)
        val koejaksoDTO1 = KoejaksoDTO()
        koejaksoDTO1.id = 1L
        val koejaksoDTO2 = KoejaksoDTO()
        assertThat(koejaksoDTO1).isNotEqualTo(koejaksoDTO2)
        koejaksoDTO2.id = koejaksoDTO1.id
        assertThat(koejaksoDTO1).isEqualTo(koejaksoDTO2)
        koejaksoDTO2.id = 2L
        assertThat(koejaksoDTO1).isNotEqualTo(koejaksoDTO2)
        koejaksoDTO1.id = null
        assertThat(koejaksoDTO1).isNotEqualTo(koejaksoDTO2)
    }
}
