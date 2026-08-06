package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
class ErikoisalaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(ErikoisalaDTO::class)
    }
}
