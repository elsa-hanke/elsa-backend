package fi.oulu.elsa.service.dto

import fi.oulu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EpaOsaamisalueDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(EpaOsaamisalueDTO::class)
        val epaOsaamisalueDTO1 = EpaOsaamisalueDTO()
        epaOsaamisalueDTO1.id = 1L
        val epaOsaamisalueDTO2 = EpaOsaamisalueDTO()
        assertThat(epaOsaamisalueDTO1).isNotEqualTo(epaOsaamisalueDTO2)
        epaOsaamisalueDTO2.id = epaOsaamisalueDTO1.id
        assertThat(epaOsaamisalueDTO1).isEqualTo(epaOsaamisalueDTO2)
        epaOsaamisalueDTO2.id = 2L
        assertThat(epaOsaamisalueDTO1).isNotEqualTo(epaOsaamisalueDTO2)
        epaOsaamisalueDTO1.id = null
        assertThat(epaOsaamisalueDTO1).isNotEqualTo(epaOsaamisalueDTO2)
    }
}
