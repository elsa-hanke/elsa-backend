package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KuntaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KuntaDTO::class)
        val kuntaDTO1 = KuntaDTO()
        kuntaDTO1.id = "1"
        val kuntaDTO2 = KuntaDTO()
        assertThat(kuntaDTO1).isNotEqualTo(kuntaDTO2)
        kuntaDTO2.id = kuntaDTO1.id
        assertThat(kuntaDTO1).isEqualTo(kuntaDTO2)
        kuntaDTO2.id = "2"
        assertThat(kuntaDTO1).isNotEqualTo(kuntaDTO2)
        kuntaDTO1.id = null
        assertThat(kuntaDTO1).isNotEqualTo(kuntaDTO2)
    }
}
