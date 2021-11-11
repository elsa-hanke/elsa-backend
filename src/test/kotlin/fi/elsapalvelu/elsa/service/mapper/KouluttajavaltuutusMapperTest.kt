package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class KouluttajavaltuutusMapperTest {

    private lateinit var kouluttajavaltuutusMapper: KouluttajavaltuutusMapper

    @BeforeEach
    fun setUp() {
        kouluttajavaltuutusMapper = KouluttajavaltuutusMapperImpl()
    }

}
