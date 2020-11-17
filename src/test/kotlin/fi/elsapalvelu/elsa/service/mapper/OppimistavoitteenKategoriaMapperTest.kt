package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppimistavoitteenKategoriaMapperTest {

    private lateinit var oppimistavoitteenKategoriaMapper: OppimistavoitteenKategoriaMapper

    @BeforeEach
    fun setUp() {
        oppimistavoitteenKategoriaMapper = OppimistavoitteenKategoriaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(oppimistavoitteenKategoriaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(oppimistavoitteenKategoriaMapper.fromId(null)).isNull()
    }
}
