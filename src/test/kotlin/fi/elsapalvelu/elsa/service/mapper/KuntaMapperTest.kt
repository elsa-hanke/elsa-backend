package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.perustiedot.KuntaMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.KuntaMapperImpl
class KuntaMapperTest {

    private lateinit var kuntaMapper: KuntaMapper

    @BeforeEach
    fun setUp() {
        kuntaMapper = KuntaMapperImpl()
    }

}
