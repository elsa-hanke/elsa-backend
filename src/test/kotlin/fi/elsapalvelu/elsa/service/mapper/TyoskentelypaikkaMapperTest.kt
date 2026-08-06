package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelypaikkaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelypaikkaMapperImpl
class TyoskentelypaikkaMapperTest {

    private lateinit var tyoskentelypaikkaMapper: TyoskentelypaikkaMapper

    @BeforeEach
    fun setUp() {
        tyoskentelypaikkaMapper = TyoskentelypaikkaMapperImpl()
    }

}
