package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavaKokonaisuusMapperImpl
class ArvioitavaKokonaisuusMapperTest {

    private lateinit var arvioitavaKokonaisuusMapper: ArvioitavaKokonaisuusMapper

    @BeforeEach
    fun setUp() {
        arvioitavaKokonaisuusMapper =
            ArvioitavaKokonaisuusMapperImpl()
    }
}
