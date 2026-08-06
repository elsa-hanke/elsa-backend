package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.mapper.kayttaja.KayttajaMapperImpl
class KayttajaMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<KayttajaMapperImpl>()
    }
}
