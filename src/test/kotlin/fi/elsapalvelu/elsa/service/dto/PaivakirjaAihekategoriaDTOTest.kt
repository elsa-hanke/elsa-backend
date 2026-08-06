package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.seuranta.PaivakirjaAihekategoriaDTO
class PaivakirjaAihekategoriaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(PaivakirjaAihekategoriaDTO::class)
    }
}
