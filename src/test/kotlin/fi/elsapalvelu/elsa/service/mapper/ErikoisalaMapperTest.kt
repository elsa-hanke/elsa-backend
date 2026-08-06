package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapperImpl
class ErikoisalaMapperTest {

    private lateinit var erikoisalaMapper: ErikoisalaMapper

    @BeforeEach
    fun setUp() {
        erikoisalaMapper = ErikoisalaMapperImpl()
    }

}
