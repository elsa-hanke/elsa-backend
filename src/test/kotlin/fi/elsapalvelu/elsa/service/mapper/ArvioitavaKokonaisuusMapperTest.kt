package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavaKokonaisuusMapperImpl
class ArvioitavaKokonaisuusMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<ArvioitavaKokonaisuusMapperImpl>()
    }
}
