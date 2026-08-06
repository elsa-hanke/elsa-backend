package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritemerkintaMapper
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritemerkintaMapperImpl
class SuoritemerkintaMapperTest {

    private lateinit var suoritemerkintaMapper: SuoritemerkintaMapper

    @BeforeEach
    fun setUp() {
        suoritemerkintaMapper = SuoritemerkintaMapperImpl()
    }

}
