package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class KayttajaMapperTest {

    private lateinit var kayttajaMapper: KayttajaMapper

    @BeforeEach
    fun setUp() {
        kayttajaMapper = KayttajaMapperImpl()
    }

}
