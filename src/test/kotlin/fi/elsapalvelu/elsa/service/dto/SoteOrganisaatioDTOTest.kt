package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SoteOrganisaatioDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(SoteOrganisaatioDTO::class)
        val soteOrganisaatioDTO1 = SoteOrganisaatioDTO()
        soteOrganisaatioDTO1.organizationId = "1.2.246.10.XXXXXXXX.10.1"
        val soteOrganisaatioDTO2 = SoteOrganisaatioDTO()
        assertThat(soteOrganisaatioDTO1).isNotEqualTo(soteOrganisaatioDTO2)
        soteOrganisaatioDTO2.organizationId = soteOrganisaatioDTO1.organizationId
        assertThat(soteOrganisaatioDTO1).isEqualTo(soteOrganisaatioDTO2)
        soteOrganisaatioDTO2.organizationId = "1.2.246.10.XXXXXXXX.10.2"
        assertThat(soteOrganisaatioDTO1).isNotEqualTo(soteOrganisaatioDTO2)
        soteOrganisaatioDTO1.organizationId = null
        assertThat(soteOrganisaatioDTO1).isNotEqualTo(soteOrganisaatioDTO2)
    }
}
