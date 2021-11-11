package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class SuoritusarviointiMapperTest {

    private lateinit var suoritusarviointiMapper: SuoritusarviointiMapper

    @BeforeEach
    fun setUp() {
        suoritusarviointiMapper = SuoritusarviointiMapperImpl()
    }

}
