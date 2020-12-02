package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KeskeytysaikaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(KeskeytysaikaDTO::class)
        val keskeytysaikaDTO1 = KeskeytysaikaDTO()
        keskeytysaikaDTO1.id = 1L
        val keskeytysaikaDTO2 = KeskeytysaikaDTO()
        assertThat(keskeytysaikaDTO1).isNotEqualTo(keskeytysaikaDTO2)
        keskeytysaikaDTO2.id = keskeytysaikaDTO1.id
        assertThat(keskeytysaikaDTO1).isEqualTo(keskeytysaikaDTO2)
        keskeytysaikaDTO2.id = 2L
        assertThat(keskeytysaikaDTO1).isNotEqualTo(keskeytysaikaDTO2)
        keskeytysaikaDTO1.id = null
        assertThat(keskeytysaikaDTO1).isNotEqualTo(keskeytysaikaDTO2)
    }
}
