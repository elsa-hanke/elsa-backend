package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.kayttaja.ErikoistuvaLaakariMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.ErikoistuvaLaakariMapperImpl
class ErikoistuvaLaakariMapperTest {

    private lateinit var erikoistuvaLaakariMapper: ErikoistuvaLaakariMapper

    @BeforeEach
    fun setUp() {
        erikoistuvaLaakariMapper = ErikoistuvaLaakariMapperImpl()
    }

}
