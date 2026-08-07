package fi.elsapalvelu.elsa.service.dto.koulutus

import fi.elsapalvelu.elsa.web.rest.dtoEqualsVerifier as verifyDtoEquals
import org.junit.jupiter.api.Test

class KoulutussuunnitelmaDTOTest {

    @Test
    fun dtoEqualsVerifier() {
        verifyDtoEquals(KoulutussuunnitelmaDTO::class)
    }
}
