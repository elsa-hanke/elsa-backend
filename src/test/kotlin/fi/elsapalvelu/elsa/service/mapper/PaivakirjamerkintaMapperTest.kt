package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class PaivakirjamerkintaMapperTest {

    private lateinit var paivakirjamerkintaMapper: PaivakirjamerkintaMapper

    @BeforeEach
    fun setUp() {
        paivakirjamerkintaMapper = PaivakirjamerkintaMapperImpl()
    }
}
