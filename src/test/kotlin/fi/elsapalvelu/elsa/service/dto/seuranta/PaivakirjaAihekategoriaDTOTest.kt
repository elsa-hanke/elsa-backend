package fi.elsapalvelu.elsa.service.dto.seuranta

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class PaivakirjaAihekategoriaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(PaivakirjaAihekategoriaDTO::class)
    }
}
