package fi.elsapalvelu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArvioitavaKokonaisuusMapperTest {

    private lateinit var arvioitavaKokonaisuusMapper: ArvioitavaKokonaisuusMapper

    @BeforeEach
    fun setUp() {
        arvioitavaKokonaisuusMapper =
            ArvioitavaKokonaisuusMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(arvioitavaKokonaisuusMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(arvioitavaKokonaisuusMapper.fromId(null)).isNull()
    }
}
