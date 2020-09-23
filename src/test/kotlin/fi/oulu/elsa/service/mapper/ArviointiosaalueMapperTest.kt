package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArviointiosaalueMapperTest {

    private lateinit var arviointiosaalueMapper: ArviointiosaalueMapper

    @BeforeEach
    fun setUp() {
        arviointiosaalueMapper = ArviointiosaalueMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(arviointiosaalueMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(arviointiosaalueMapper.fromId(null)).isNull()
    }
}
