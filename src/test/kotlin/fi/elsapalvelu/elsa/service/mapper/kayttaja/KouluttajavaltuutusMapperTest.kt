package fi.elsapalvelu.elsa.service.mapper.kayttaja

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class KouluttajavaltuutusMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<KouluttajavaltuutusMapperImpl>()
    }
}
