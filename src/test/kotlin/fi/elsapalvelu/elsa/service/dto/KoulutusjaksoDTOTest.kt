package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutusjaksoDTO
class KoulutusjaksoDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(KoulutusjaksoDTO::class)
    }
}
