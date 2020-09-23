package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KayttajaMapperTest {

    private lateinit var kayttajaMapper: KayttajaMapper

    @BeforeEach
    fun setUp() {
        kayttajaMapper = KayttajaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(kayttajaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(kayttajaMapper.fromId(null)).isNull()
    }
}
