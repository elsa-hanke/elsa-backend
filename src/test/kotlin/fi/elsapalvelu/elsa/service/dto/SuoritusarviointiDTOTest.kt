package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiDTO
class SuoritusarviointiDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(SuoritusarviointiDTO::class)
    }
}
