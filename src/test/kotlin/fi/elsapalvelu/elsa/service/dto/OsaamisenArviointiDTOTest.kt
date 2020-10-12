package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OsaamisenArviointiDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(OsaamisenArviointiDTO::class)
        val osaamisenArviointiDTO1 = OsaamisenArviointiDTO()
        osaamisenArviointiDTO1.id = 1L
        val osaamisenArviointiDTO2 = OsaamisenArviointiDTO()
        assertThat(osaamisenArviointiDTO1).isNotEqualTo(osaamisenArviointiDTO2)
        osaamisenArviointiDTO2.id = osaamisenArviointiDTO1.id
        assertThat(osaamisenArviointiDTO1).isEqualTo(osaamisenArviointiDTO2)
        osaamisenArviointiDTO2.id = 2L
        assertThat(osaamisenArviointiDTO1).isNotEqualTo(osaamisenArviointiDTO2)
        osaamisenArviointiDTO1.id = null
        assertThat(osaamisenArviointiDTO1).isNotEqualTo(osaamisenArviointiDTO2)
    }
}
