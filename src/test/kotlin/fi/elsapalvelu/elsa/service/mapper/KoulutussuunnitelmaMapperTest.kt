package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutussuunnitelmaMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutussuunnitelmaMapperImpl
class KoulutussuunnitelmaMapperTest {

    private lateinit var koulutussuunnitelmaMapper: KoulutussuunnitelmaMapper

    @BeforeEach
    fun setUp() {
        koulutussuunnitelmaMapper = KoulutussuunnitelmaMapperImpl()
    }
}
