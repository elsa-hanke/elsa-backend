package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class AsiakirjaMapperTest {
    private lateinit var asiakirjaMapper: AsiakirjaMapper

    @BeforeEach
    fun setUp() {
        asiakirjaMapper = AsiakirjaMapperImpl()
    }

}
