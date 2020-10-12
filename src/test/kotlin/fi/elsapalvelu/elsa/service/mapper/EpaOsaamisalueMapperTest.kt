package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpaOsaamisalueMapperTest {

    private lateinit var epaOsaamisalueMapper: EpaOsaamisalueMapper

    @BeforeEach
    fun setUp() {
        epaOsaamisalueMapper = EpaOsaamisalueMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(epaOsaamisalueMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(epaOsaamisalueMapper.fromId(null)).isNull()
    }
}
