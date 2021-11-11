package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class ErikoistuvaLaakariMapperTest {

    private lateinit var erikoistuvaLaakariMapper: ErikoistuvaLaakariMapper

    @BeforeEach
    fun setUp() {
        erikoistuvaLaakariMapper = ErikoistuvaLaakariMapperImpl()
    }

}
