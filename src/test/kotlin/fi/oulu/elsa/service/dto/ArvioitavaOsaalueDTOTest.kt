package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArvioitavaOsaalueDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(ArvioitavaOsaalueDTO::class)
        val arvioitavaOsaalueDTO1 = ArvioitavaOsaalueDTO()
        arvioitavaOsaalueDTO1.id = 1L
        val arvioitavaOsaalueDTO2 = ArvioitavaOsaalueDTO()
        assertThat(arvioitavaOsaalueDTO1).isNotEqualTo(arvioitavaOsaalueDTO2)
        arvioitavaOsaalueDTO2.id = arvioitavaOsaalueDTO1.id
        assertThat(arvioitavaOsaalueDTO1).isEqualTo(arvioitavaOsaalueDTO2)
        arvioitavaOsaalueDTO2.id = 2L
        assertThat(arvioitavaOsaalueDTO1).isNotEqualTo(arvioitavaOsaalueDTO2)
        arvioitavaOsaalueDTO1.id = null
        assertThat(arvioitavaOsaalueDTO1).isNotEqualTo(arvioitavaOsaalueDTO2)
    }
}
