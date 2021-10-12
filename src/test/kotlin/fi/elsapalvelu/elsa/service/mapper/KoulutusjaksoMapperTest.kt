package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class KoulutusjaksoMapperTest {

    private lateinit var koulutusjaksoMapper: KoulutusjaksoMapper

    @BeforeEach
    fun setUp() {
        koulutusjaksoMapper = KoulutusjaksoMapperImpl()
    }
}
