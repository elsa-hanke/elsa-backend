package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.seuranta.PaivakirjamerkintaDTO
class PaivakirjamerkintaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(PaivakirjamerkintaDTO::class)
    }
}
