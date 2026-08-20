package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class SuoritusarvioinninKommenttiMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<SuoritusarvioinninKommenttiMapperImpl>()
    }
}
