package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.tyoskentely.PoissaolonSyyMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.PoissaolonSyyMapperImpl
class PoissaolonSyyMapperTest {

    private lateinit var poissaolonSyyMapper: PoissaolonSyyMapper

    @BeforeEach
    fun setUp() {
        poissaolonSyyMapper = PoissaolonSyyMapperImpl()
    }

}
