package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OsaalueenArviointiDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(OsaalueenArviointiDTO::class)
        val osaalueenArviointiDTO1 = OsaalueenArviointiDTO()
        osaalueenArviointiDTO1.id = 1L
        val osaalueenArviointiDTO2 = OsaalueenArviointiDTO()
        assertThat(osaalueenArviointiDTO1).isNotEqualTo(osaalueenArviointiDTO2)
        osaalueenArviointiDTO2.id = osaalueenArviointiDTO1.id
        assertThat(osaalueenArviointiDTO1).isEqualTo(osaalueenArviointiDTO2)
        osaalueenArviointiDTO2.id = 2L
        assertThat(osaalueenArviointiDTO1).isNotEqualTo(osaalueenArviointiDTO2)
        osaalueenArviointiDTO1.id = null
        assertThat(osaalueenArviointiDTO1).isNotEqualTo(osaalueenArviointiDTO2)
    }
}
