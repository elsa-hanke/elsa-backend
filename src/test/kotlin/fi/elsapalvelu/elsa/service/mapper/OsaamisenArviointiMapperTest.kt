package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OsaamisenArviointiMapperTest {

    private lateinit var osaamisenArviointiMapper: OsaamisenArviointiMapper

    @BeforeEach
    fun setUp() {
        osaamisenArviointiMapper = OsaamisenArviointiMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(osaamisenArviointiMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(osaamisenArviointiMapper.fromId(null)).isNull()
    }
}
