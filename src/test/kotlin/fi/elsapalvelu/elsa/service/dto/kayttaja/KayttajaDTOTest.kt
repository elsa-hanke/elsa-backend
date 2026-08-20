package fi.elsapalvelu.elsa.service.dto.kayttaja

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class KayttajaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(KayttajaDTO::class)
    }
}
