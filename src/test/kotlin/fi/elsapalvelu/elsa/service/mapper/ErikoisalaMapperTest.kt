package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class ErikoisalaMapperTest {

    private lateinit var erikoisalaMapper: ErikoisalaMapper

    @BeforeEach
    fun setUp() {
        erikoisalaMapper = ErikoisalaMapperImpl()
    }

}
