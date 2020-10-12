package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EpaOsaamisalueTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(EpaOsaamisalue::class)
        val epaOsaamisalue1 = EpaOsaamisalue()
        epaOsaamisalue1.id = 1L
        val epaOsaamisalue2 = EpaOsaamisalue()
        epaOsaamisalue2.id = epaOsaamisalue1.id
        assertThat(epaOsaamisalue1).isEqualTo(epaOsaamisalue2)
        epaOsaamisalue2.id = 2L
        assertThat(epaOsaamisalue1).isNotEqualTo(epaOsaamisalue2)
        epaOsaamisalue1.id = null
        assertThat(epaOsaamisalue1).isNotEqualTo(epaOsaamisalue2)
    }
}
