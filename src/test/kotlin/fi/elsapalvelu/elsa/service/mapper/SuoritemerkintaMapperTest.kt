package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SuoritemerkintaMapperTest {

    private lateinit var suoritemerkintaMapper: SuoritemerkintaMapper

    @BeforeEach
    fun setUp() {
        suoritemerkintaMapper = SuoritemerkintaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(suoritemerkintaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(suoritemerkintaMapper.fromId(null)).isNull()
    }
}
