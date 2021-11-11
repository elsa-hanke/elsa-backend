package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class SuoritemerkintaMapperTest {

    private lateinit var suoritemerkintaMapper: SuoritemerkintaMapper

    @BeforeEach
    fun setUp() {
        suoritemerkintaMapper = SuoritemerkintaMapperImpl()
    }

}
