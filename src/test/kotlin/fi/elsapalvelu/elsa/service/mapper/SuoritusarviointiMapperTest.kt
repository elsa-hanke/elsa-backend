package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SuoritusarviointiMapperTest {

    private lateinit var suoritusarviointiMapper: SuoritusarviointiMapper

    @BeforeEach
    fun setUp() {
        suoritusarviointiMapper = SuoritusarviointiMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(suoritusarviointiMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(suoritusarviointiMapper.fromId(null)).isNull()
    }
}
