package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArvioitavaOsaalueMapperTest {

    private lateinit var arvioitavaOsaalueMapper: ArvioitavaOsaalueMapper

    @BeforeEach
    fun setUp() {
        arvioitavaOsaalueMapper = ArvioitavaOsaalueMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(arvioitavaOsaalueMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(arvioitavaOsaalueMapper.fromId(null)).isNull()
    }
}
