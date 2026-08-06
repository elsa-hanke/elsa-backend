package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
class KayttajaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(KayttajaDTO::class)
    }
}
