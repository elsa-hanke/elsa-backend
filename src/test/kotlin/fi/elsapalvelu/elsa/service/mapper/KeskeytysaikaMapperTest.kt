package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KeskeytysaikaMapperTest {

    private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper

    @BeforeEach
    fun setUp() {
        keskeytysaikaMapper = KeskeytysaikaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(keskeytysaikaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(keskeytysaikaMapper.fromId(null)).isNull()
    }
}
