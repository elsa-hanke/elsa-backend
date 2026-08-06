package fi.elsapalvelu.elsa.service.dto.tyoskentely

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class TyoskentelyjaksoDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(TyoskentelyjaksoDTO::class)
    }
}
