package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class TyoskentelyjaksoMapperTest {

    private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

    @BeforeEach
    fun setUp() {
        tyoskentelyjaksoMapper = TyoskentelyjaksoMapperImpl()
    }

}
