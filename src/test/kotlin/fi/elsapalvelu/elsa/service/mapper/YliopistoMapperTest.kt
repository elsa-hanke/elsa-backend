package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class YliopistoMapperTest {

    private lateinit var yliopistoMapper: YliopistoMapper

    @BeforeEach
    fun setUp() {
        yliopistoMapper = YliopistoMapperImpl()
    }

}
