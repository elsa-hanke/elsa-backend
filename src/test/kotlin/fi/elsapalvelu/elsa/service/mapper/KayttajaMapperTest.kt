package fi.elsapalvelu.elsa.service.mapper

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
        val id = "b3863335-71c8-4e0d-8c45-82f06991e285"
        assertThat(kayttajaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(kayttajaMapper.fromId(null)).isNull()
    }
}
