package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistuvaLaakariDTO
class ErikoistuvaLaakariDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(ErikoistuvaLaakariDTO::class)
    }
}
