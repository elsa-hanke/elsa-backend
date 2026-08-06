package fi.elsapalvelu.elsa.service.dto.tyoskentely

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class KeskeytysaikaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(KeskeytysaikaDTO::class)
    }
}
