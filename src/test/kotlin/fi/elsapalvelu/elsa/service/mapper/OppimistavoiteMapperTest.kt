package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class OppimistavoiteMapperTest {

    private lateinit var oppimistavoiteMapper: OppimistavoiteMapper

    @BeforeEach
    fun setUp() {
        oppimistavoiteMapper = OppimistavoiteMapperImpl()
    }

}
