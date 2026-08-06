package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.seuranta.PaivakirjaAihekategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.seuranta.PaivakirjaAihekategoriaMapperImpl
class PaivakirjaAihekategoriaMapperTest {

    private lateinit var paivakirjaAihekategoriaMapper: PaivakirjaAihekategoriaMapper

    @BeforeEach
    fun setUp() {
        paivakirjaAihekategoriaMapper = PaivakirjaAihekategoriaMapperImpl()
    }
}
