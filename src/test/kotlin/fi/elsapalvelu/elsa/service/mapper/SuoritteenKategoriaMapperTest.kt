package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class SuoritteenKategoriaMapperTest {

    private lateinit var suoritteenKategoriaMapper: SuoritteenKategoriaMapper

    @BeforeEach
    fun setUp() {
        suoritteenKategoriaMapper = SuoritteenKategoriaMapperImpl()
    }
}
