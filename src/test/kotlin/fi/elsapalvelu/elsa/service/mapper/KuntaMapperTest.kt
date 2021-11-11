package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class KuntaMapperTest {

    private lateinit var kuntaMapper: KuntaMapper

    @BeforeEach
    fun setUp() {
        kuntaMapper = KuntaMapperImpl()
    }

}
