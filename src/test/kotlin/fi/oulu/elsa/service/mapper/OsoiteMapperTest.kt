package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OsoiteMapperTest {

    private lateinit var osoiteMapper: OsoiteMapper

    @BeforeEach
    fun setUp() {
        osoiteMapper = OsoiteMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(osoiteMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(osoiteMapper.fromId(null)).isNull()
    }
}
