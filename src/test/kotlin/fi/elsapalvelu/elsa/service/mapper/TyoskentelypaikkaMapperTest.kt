package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TyoskentelypaikkaMapperTest {

    private lateinit var tyoskentelypaikkaMapper: TyoskentelypaikkaMapper

    @BeforeEach
    fun setUp() {
        tyoskentelypaikkaMapper = TyoskentelypaikkaMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(tyoskentelypaikkaMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(tyoskentelypaikkaMapper.fromId(null)).isNull()
    }
}
