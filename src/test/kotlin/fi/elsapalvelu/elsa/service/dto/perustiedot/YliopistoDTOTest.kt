package fi.elsapalvelu.elsa.service.dto.perustiedot

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class YliopistoDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(YliopistoDTO::class)
    }
}
