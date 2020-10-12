package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PikaviestiMapperTest {

    private lateinit var pikaviestiMapper: PikaviestiMapper

    @BeforeEach
    fun setUp() {
        pikaviestiMapper = PikaviestiMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(pikaviestiMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(pikaviestiMapper.fromId(null)).isNull()
    }
}
