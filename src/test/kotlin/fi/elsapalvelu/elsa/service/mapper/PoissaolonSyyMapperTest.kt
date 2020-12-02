package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PoissaolonSyyMapperTest {

    private lateinit var poissaolonSyyMapper: PoissaolonSyyMapper

    @BeforeEach
    fun setUp() {
        poissaolonSyyMapper = PoissaolonSyyMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(poissaolonSyyMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(poissaolonSyyMapper.fromId(null)).isNull()
    }
}
