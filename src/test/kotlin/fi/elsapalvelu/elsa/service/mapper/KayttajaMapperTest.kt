package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.kayttaja.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.KayttajaMapperImpl
class KayttajaMapperTest {

    private lateinit var kayttajaMapper: KayttajaMapper

    @BeforeEach
    fun setUp() {
        kayttajaMapper = KayttajaMapperImpl()
    }

}
