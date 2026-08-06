package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritteenKategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritteenKategoriaMapperImpl
class SuoritteenKategoriaMapperTest {

    private lateinit var suoritteenKategoriaMapper: SuoritteenKategoriaMapper

    @BeforeEach
    fun setUp() {
        suoritteenKategoriaMapper = SuoritteenKategoriaMapperImpl()
    }
}
