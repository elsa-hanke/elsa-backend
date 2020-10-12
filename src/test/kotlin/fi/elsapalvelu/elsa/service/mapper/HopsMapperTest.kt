package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HopsMapperTest {

    private lateinit var hopsMapper: HopsMapper

    @BeforeEach
    fun setUp() {
        hopsMapper = HopsMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(hopsMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(hopsMapper.fromId(null)).isNull()
    }
}
