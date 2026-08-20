package fi.elsapalvelu.elsa.service.dto.arviointi

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class ArvioitavaKokonaisuusDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(ArvioitavaKokonaisuusDTO::class)
    }
}
