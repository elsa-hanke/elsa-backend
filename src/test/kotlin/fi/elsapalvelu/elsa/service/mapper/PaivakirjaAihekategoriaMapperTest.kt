package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class PaivakirjaAihekategoriaMapperTest {

    private lateinit var paivakirjaAihekategoriaMapper: PaivakirjaAihekategoriaMapper

    @BeforeEach
    fun setUp() {
        paivakirjaAihekategoriaMapper = PaivakirjaAihekategoriaMapperImpl()
    }
}
