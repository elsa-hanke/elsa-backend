package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TyoskentelyjaksoMapperTest {

    private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

    @BeforeEach
    fun setUp() {
        tyoskentelyjaksoMapper = TyoskentelyjaksoMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(tyoskentelyjaksoMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(tyoskentelyjaksoMapper.fromId(null)).isNull()
    }
}
