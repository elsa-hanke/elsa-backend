package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OsaalueenArviointiMapperTest {

    private lateinit var osaalueenArviointiMapper: OsaalueenArviointiMapper

    @BeforeEach
    fun setUp() {
        osaalueenArviointiMapper = OsaalueenArviointiMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(osaalueenArviointiMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(osaalueenArviointiMapper.fromId(null)).isNull()
    }
}
