package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelyjaksoMapperImpl
class TyoskentelyjaksoMapperTest {

    private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

    @BeforeEach
    fun setUp() {
        tyoskentelyjaksoMapper = TyoskentelyjaksoMapperImpl()
    }

}
