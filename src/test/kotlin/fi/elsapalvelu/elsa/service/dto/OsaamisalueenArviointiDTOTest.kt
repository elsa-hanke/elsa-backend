package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OsaamisalueenArviointiDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(OsaamisalueenArviointiDTO::class)
        val osaamisalueenArviointiDTO1 = OsaamisalueenArviointiDTO()
        osaamisalueenArviointiDTO1.id = 1L
        val osaamisalueenArviointiDTO2 = OsaamisalueenArviointiDTO()
        assertThat(osaamisalueenArviointiDTO1).isNotEqualTo(osaamisalueenArviointiDTO2)
        osaamisalueenArviointiDTO2.id = osaamisalueenArviointiDTO1.id
        assertThat(osaamisalueenArviointiDTO1).isEqualTo(osaamisalueenArviointiDTO2)
        osaamisalueenArviointiDTO2.id = 2L
        assertThat(osaamisalueenArviointiDTO1).isNotEqualTo(osaamisalueenArviointiDTO2)
        osaamisalueenArviointiDTO1.id = null
        assertThat(osaamisalueenArviointiDTO1).isNotEqualTo(osaamisalueenArviointiDTO2)
    }
}
