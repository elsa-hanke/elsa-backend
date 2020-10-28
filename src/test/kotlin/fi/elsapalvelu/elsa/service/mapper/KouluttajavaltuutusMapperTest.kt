package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KouluttajavaltuutusMapperTest {

    private lateinit var kouluttajavaltuutusMapper: KouluttajavaltuutusMapper

    @BeforeEach
    fun setUp() {
        kouluttajavaltuutusMapper = KouluttajavaltuutusMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(kouluttajavaltuutusMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(kouluttajavaltuutusMapper.fromId(null)).isNull()
    }
}
