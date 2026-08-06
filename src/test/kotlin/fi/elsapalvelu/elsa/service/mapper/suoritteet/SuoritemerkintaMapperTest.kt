package fi.elsapalvelu.elsa.service.mapper.suoritteet

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class SuoritemerkintaMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<SuoritemerkintaMapperImpl>()
    }
}
