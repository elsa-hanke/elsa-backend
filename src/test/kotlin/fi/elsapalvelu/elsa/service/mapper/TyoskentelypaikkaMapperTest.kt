package fi.elsapalvelu.elsa.service.mapper

import org.junit.jupiter.api.BeforeEach

class TyoskentelypaikkaMapperTest {

    private lateinit var tyoskentelypaikkaMapper: TyoskentelypaikkaMapper

    @BeforeEach
    fun setUp() {
        tyoskentelypaikkaMapper = TyoskentelypaikkaMapperImpl()
    }

}
