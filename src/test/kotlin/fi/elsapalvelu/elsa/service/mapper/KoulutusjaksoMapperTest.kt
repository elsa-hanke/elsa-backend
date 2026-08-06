package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutusjaksoMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutusjaksoMapperImpl
class KoulutusjaksoMapperTest {

    private lateinit var koulutusjaksoMapper: KoulutusjaksoMapper

    @BeforeEach
    fun setUp() {
        koulutusjaksoMapper = KoulutusjaksoMapperImpl()
    }
}
