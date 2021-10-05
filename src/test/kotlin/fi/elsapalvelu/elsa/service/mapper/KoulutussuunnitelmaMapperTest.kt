package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class KoulutussuunnitelmaMapperTest {

    private lateinit var koulutussuunnitelmaMapper: KoulutussuunnitelmaMapper

    @BeforeEach
    fun setUp() {
        koulutussuunnitelmaMapper = KoulutussuunnitelmaMapperImpl()
    }
}
