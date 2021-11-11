package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class OppimistavoitteenKategoriaMapperTest {

    private lateinit var oppimistavoitteenKategoriaMapper: OppimistavoitteenKategoriaMapper

    @BeforeEach
    fun setUp() {
        oppimistavoitteenKategoriaMapper = OppimistavoitteenKategoriaMapperImpl()
    }
}
