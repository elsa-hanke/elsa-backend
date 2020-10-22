package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SoteOrganisaatioTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(SoteOrganisaatio::class)
        val soteOrganisaatio1 = SoteOrganisaatio()
        soteOrganisaatio1.organizationId = "1.2.246.10.XXXXXXXX.10.1"
        val soteOrganisaatio2 = SoteOrganisaatio()
        soteOrganisaatio2.organizationId = soteOrganisaatio1.organizationId
        assertThat(soteOrganisaatio1).isEqualTo(soteOrganisaatio2)
        soteOrganisaatio2.organizationId = "1.2.246.10.XXXXXXXX.10.2"
        assertThat(soteOrganisaatio1).isNotEqualTo(soteOrganisaatio2)
        soteOrganisaatio1.organizationId = null
        assertThat(soteOrganisaatio1).isNotEqualTo(soteOrganisaatio2)
    }
}
