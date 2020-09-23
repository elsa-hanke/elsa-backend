package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class YliopistoMapperTest {

    private lateinit var yliopistoMapper: YliopistoMapper

    @BeforeEach
    fun setUp() {
        yliopistoMapper = YliopistoMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(yliopistoMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(yliopistoMapper.fromId(null)).isNull()
    }
}
