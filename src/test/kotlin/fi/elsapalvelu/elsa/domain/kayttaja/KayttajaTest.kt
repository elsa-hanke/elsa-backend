package fi.elsapalvelu.elsa.domain.kayttaja

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class KayttajaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Kayttaja::class)
    }
}
