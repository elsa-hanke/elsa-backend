package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PikaviestiDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(PikaviestiDTO::class)
        val pikaviestiDTO1 = PikaviestiDTO()
        pikaviestiDTO1.id = 1L
        val pikaviestiDTO2 = PikaviestiDTO()
        assertThat(pikaviestiDTO1).isNotEqualTo(pikaviestiDTO2)
        pikaviestiDTO2.id = pikaviestiDTO1.id
        assertThat(pikaviestiDTO1).isEqualTo(pikaviestiDTO2)
        pikaviestiDTO2.id = 2L
        assertThat(pikaviestiDTO1).isNotEqualTo(pikaviestiDTO2)
        pikaviestiDTO1.id = null
        assertThat(pikaviestiDTO1).isNotEqualTo(pikaviestiDTO2)
    }
}
