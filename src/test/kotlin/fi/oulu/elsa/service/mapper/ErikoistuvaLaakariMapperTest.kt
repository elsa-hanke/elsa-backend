package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ErikoistuvaLaakariMapperTest {

    private lateinit var erikoistuvaLaakariMapper: ErikoistuvaLaakariMapper

    @BeforeEach
    fun setUp() {
        erikoistuvaLaakariMapper = ErikoistuvaLaakariMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(erikoistuvaLaakariMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(erikoistuvaLaakariMapper.fromId(null)).isNull()
    }
}
