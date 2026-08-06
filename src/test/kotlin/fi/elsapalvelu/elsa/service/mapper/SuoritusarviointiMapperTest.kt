package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarviointiMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarviointiMapperImpl
class SuoritusarviointiMapperTest {

    private lateinit var suoritusarviointiMapper: SuoritusarviointiMapper

    @BeforeEach
    fun setUp() {
        suoritusarviointiMapper = SuoritusarviointiMapperImpl()
    }

}
