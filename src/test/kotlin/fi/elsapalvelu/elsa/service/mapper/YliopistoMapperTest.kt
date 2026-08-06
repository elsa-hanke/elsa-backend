package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.perustiedot.YliopistoMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.YliopistoMapperImpl
class YliopistoMapperTest {

    private lateinit var yliopistoMapper: YliopistoMapper

    @BeforeEach
    fun setUp() {
        yliopistoMapper = YliopistoMapperImpl()
    }

}
