package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoriteMapper
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoriteMapperImpl
class SuoriteMapperTest {

    private lateinit var suoriteMapper: SuoriteMapper

    @BeforeEach
    fun setUp() {
        suoriteMapper = SuoriteMapperImpl()
    }

}
