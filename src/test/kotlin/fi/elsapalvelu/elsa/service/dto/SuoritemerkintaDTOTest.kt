package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritemerkintaDTO
class SuoritemerkintaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(SuoritemerkintaDTO::class)
    }
}
