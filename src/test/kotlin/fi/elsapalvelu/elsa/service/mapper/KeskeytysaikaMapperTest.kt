package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.tyoskentely.KeskeytysaikaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.KeskeytysaikaMapperImpl
class KeskeytysaikaMapperTest {

    private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper

    @BeforeEach
    fun setUp() {
        keskeytysaikaMapper = KeskeytysaikaMapperImpl()
    }

}
