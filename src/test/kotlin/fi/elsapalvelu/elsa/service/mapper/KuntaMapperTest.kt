package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KuntaMapperTest {

    private lateinit var kuntaMapper: KuntaMapper

    @BeforeEach
    fun setUp() {
        kuntaMapper = KuntaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = "1"
        assertThat(kuntaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(kuntaMapper.fromId(null)).isNull()
    }
}
