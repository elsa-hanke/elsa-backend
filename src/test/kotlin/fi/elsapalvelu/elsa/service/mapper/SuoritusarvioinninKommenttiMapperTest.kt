package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarvioinninKommenttiMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarvioinninKommenttiMapperImpl
class SuoritusarvioinninKommenttiMapperTest {

    private lateinit var suoritusarvioinninKommenttiMapper: SuoritusarvioinninKommenttiMapper

    @BeforeEach
    fun setUp() {
        suoritusarvioinninKommenttiMapper = SuoritusarvioinninKommenttiMapperImpl()
    }

}
