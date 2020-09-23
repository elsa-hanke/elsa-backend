package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OsaamisalueenArviointiMapperTest {

    private lateinit var osaamisalueenArviointiMapper: OsaamisalueenArviointiMapper

    @BeforeEach
    fun setUp() {
        osaamisalueenArviointiMapper = OsaamisalueenArviointiMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(osaamisalueenArviointiMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(osaamisalueenArviointiMapper.fromId(null)).isNull()
    }
}
