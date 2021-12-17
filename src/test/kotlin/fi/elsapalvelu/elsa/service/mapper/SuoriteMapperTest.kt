package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class SuoriteMapperTest {

    private lateinit var suoriteMapper: SuoriteMapper

    @BeforeEach
    fun setUp() {
        suoriteMapper = SuoriteMapperImpl()
    }

}
