package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.seuranta.PaivakirjamerkintaMapper
import fi.elsapalvelu.elsa.service.mapper.seuranta.PaivakirjamerkintaMapperImpl
class PaivakirjamerkintaMapperTest {

    private lateinit var paivakirjamerkintaMapper: PaivakirjamerkintaMapper

    @BeforeEach
    fun setUp() {
        paivakirjamerkintaMapper = PaivakirjamerkintaMapperImpl()
    }
}
