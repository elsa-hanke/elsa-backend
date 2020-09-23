package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpintooikeustiedotMapperTest {

    private lateinit var opintooikeustiedotMapper: OpintooikeustiedotMapper

    @BeforeEach
    fun setUp() {
        opintooikeustiedotMapper = OpintooikeustiedotMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(opintooikeustiedotMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(opintooikeustiedotMapper.fromId(null)).isNull()
    }
}
