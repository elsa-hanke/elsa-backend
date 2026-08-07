package fi.elsapalvelu.elsa.service.mapper.seuranta

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

class PaivakirjaAihekategoriaMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<PaivakirjaAihekategoriaMapperImpl>()
    }
}
