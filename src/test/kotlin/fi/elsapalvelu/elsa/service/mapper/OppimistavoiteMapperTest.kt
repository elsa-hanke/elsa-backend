package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppimistavoiteMapperTest {

    private lateinit var oppimistavoiteMapper: OppimistavoiteMapper

    @BeforeEach
    fun setUp() {
        oppimistavoiteMapper = OppimistavoiteMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(oppimistavoiteMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(oppimistavoiteMapper.fromId(null)).isNull()
    }
}
