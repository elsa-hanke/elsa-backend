package fi.elsapalvelu.elsa.service.mapper.perustiedot

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class ErikoisalaMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<ErikoisalaMapperImpl>()
    }
}
