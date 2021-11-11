package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class PoissaolonSyyMapperTest {

    private lateinit var poissaolonSyyMapper: PoissaolonSyyMapper

    @BeforeEach
    fun setUp() {
        poissaolonSyyMapper = PoissaolonSyyMapperImpl()
    }

}
