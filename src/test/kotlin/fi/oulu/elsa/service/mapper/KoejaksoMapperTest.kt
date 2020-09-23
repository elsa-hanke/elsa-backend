package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KoejaksoMapperTest {

    private lateinit var koejaksoMapper: KoejaksoMapper

    @BeforeEach
    fun setUp() {
        koejaksoMapper = KoejaksoMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(koejaksoMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(koejaksoMapper.fromId(null)).isNull()
    }
}
