package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class KeskeytysaikaMapperTest {

    private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper

    @BeforeEach
    fun setUp() {
        keskeytysaikaMapper = KeskeytysaikaMapperImpl()
    }

}
