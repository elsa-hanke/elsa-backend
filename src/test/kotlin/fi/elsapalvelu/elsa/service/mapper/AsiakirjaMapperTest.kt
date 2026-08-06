package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapperImpl
class AsiakirjaMapperTest {
    private lateinit var asiakirjaMapper: AsiakirjaMapper

    @BeforeEach
    fun setUp() {
        asiakirjaMapper = AsiakirjaMapperImpl()
    }

}
