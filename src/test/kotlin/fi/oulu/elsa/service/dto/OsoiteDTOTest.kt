package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OsoiteDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(OsoiteDTO::class)
        val osoiteDTO1 = OsoiteDTO()
        osoiteDTO1.id = 1L
        val osoiteDTO2 = OsoiteDTO()
        assertThat(osoiteDTO1).isNotEqualTo(osoiteDTO2)
        osoiteDTO2.id = osoiteDTO1.id
        assertThat(osoiteDTO1).isEqualTo(osoiteDTO2)
        osoiteDTO2.id = 2L
        assertThat(osoiteDTO1).isNotEqualTo(osoiteDTO2)
        osoiteDTO1.id = null
        assertThat(osoiteDTO1).isNotEqualTo(osoiteDTO2)
    }
}
