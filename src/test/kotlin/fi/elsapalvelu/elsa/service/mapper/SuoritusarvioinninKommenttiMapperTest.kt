package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SuoritusarvioinninKommenttiMapperTest {

    private lateinit var suoritusarvioinninKommenttiMapper: SuoritusarvioinninKommenttiMapper

    @BeforeEach
    fun setUp() {
        suoritusarvioinninKommenttiMapper = SuoritusarvioinninKommenttiMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(suoritusarvioinninKommenttiMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(suoritusarvioinninKommenttiMapper.fromId(null)).isNull()
    }
}
