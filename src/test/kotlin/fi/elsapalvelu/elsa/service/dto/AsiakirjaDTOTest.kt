package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AsiakirjaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(AsiakirjaDTO::class)
        val asiakirjaDTO1 = AsiakirjaDTO()
        asiakirjaDTO1.id = 1L
        val asiakirjaDTO2 = AsiakirjaDTO()
        assertThat(asiakirjaDTO1).isNotEqualTo(asiakirjaDTO2)
        asiakirjaDTO2.id = asiakirjaDTO1.id
        assertThat(asiakirjaDTO1).isEqualTo(asiakirjaDTO2)
        asiakirjaDTO2.id = 2L
        assertThat(asiakirjaDTO1).isNotEqualTo(asiakirjaDTO2)
        asiakirjaDTO1.id = null
        assertThat(asiakirjaDTO1).isNotEqualTo(asiakirjaDTO2)
    }
}
