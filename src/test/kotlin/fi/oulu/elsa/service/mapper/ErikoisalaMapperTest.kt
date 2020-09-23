package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ErikoisalaMapperTest {

    private lateinit var erikoisalaMapper: ErikoisalaMapper

    @BeforeEach
    fun setUp() {
        erikoisalaMapper = ErikoisalaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(erikoisalaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(erikoisalaMapper.fromId(null)).isNull()
    }
}
