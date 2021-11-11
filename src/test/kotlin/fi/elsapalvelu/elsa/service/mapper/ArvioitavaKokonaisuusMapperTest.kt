package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class ArvioitavaKokonaisuusMapperTest {

    private lateinit var arvioitavaKokonaisuusMapper: ArvioitavaKokonaisuusMapper

    @BeforeEach
    fun setUp() {
        arvioitavaKokonaisuusMapper =
            ArvioitavaKokonaisuusMapperImpl()
    }
}
