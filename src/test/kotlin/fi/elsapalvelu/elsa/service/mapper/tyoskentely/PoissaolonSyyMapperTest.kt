package fi.elsapalvelu.elsa.service.mapper.tyoskentely

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class PoissaolonSyyMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<PoissaolonSyyMapperImpl>()
    }
}
