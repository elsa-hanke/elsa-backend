package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SoteOrganisaatioMapperTest {

    private lateinit var soteOrganisaatioMapper: SoteOrganisaatioMapper

    @BeforeEach
    fun setUp() {
        soteOrganisaatioMapper = SoteOrganisaatioMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val organizationId = "1.2.246.10.XXXXXXXX.10.1"

        assertThat(soteOrganisaatioMapper.fromId(organizationId)?.organizationId).isEqualTo(organizationId)
        assertThat(soteOrganisaatioMapper.fromId(null)).isNull()
    }
}
