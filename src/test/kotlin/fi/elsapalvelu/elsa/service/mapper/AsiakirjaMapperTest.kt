package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsiakirjaMapperTest {
    private lateinit var asiakirjaMapper: AsiakirjaMapper

    @BeforeEach
    fun setUp() {
        asiakirjaMapper = AsiakirjaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(asiakirjaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(asiakirjaMapper.fromId(null)).isNull()
    }
}
