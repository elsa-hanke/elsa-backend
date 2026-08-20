package fi.elsapalvelu.elsa.service.mapper.suoritteet

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class SuoritteenKategoriaMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<SuoritteenKategoriaMapperImpl>()
    }
}
