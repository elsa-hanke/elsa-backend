package fi.elsapalvelu.elsa.domain.kayttaja

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class KouluttajavaltuutusTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Kouluttajavaltuutus::class)
    }
}
