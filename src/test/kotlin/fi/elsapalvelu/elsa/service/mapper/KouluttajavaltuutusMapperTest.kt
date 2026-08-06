package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.kayttaja.KouluttajavaltuutusMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.KouluttajavaltuutusMapperImpl
class KouluttajavaltuutusMapperTest {

    private lateinit var kouluttajavaltuutusMapper: KouluttajavaltuutusMapper

    @BeforeEach
    fun setUp() {
        kouluttajavaltuutusMapper = KouluttajavaltuutusMapperImpl()
    }

}
