package fi.elsapalvelu.elsa.service.dto.suoritteet

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class SuoritemerkintaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(SuoritemerkintaDTO::class)
    }
}
