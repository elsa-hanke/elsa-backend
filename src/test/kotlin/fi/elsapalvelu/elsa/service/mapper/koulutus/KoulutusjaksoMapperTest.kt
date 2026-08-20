package fi.elsapalvelu.elsa.service.mapper.koulutus

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class KoulutusjaksoMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<KoulutusjaksoMapperImpl>()
    }
}
