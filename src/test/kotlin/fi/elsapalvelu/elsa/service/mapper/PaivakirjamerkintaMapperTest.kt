package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.web.rest.mapperVerifier as verifyMapper
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.mapper.seuranta.PaivakirjamerkintaMapperImpl
class PaivakirjamerkintaMapperTest {

    @Test
    fun mapperVerifier() {
        verifyMapper<PaivakirjamerkintaMapperImpl>()
    }
}
