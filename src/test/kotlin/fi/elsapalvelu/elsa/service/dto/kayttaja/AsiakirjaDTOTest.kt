package fi.elsapalvelu.elsa.service.dto.kayttaja

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class AsiakirjaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(AsiakirjaDTO::class)
    }
}
