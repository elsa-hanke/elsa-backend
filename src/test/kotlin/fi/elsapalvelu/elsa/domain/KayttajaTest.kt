package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
class KayttajaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Kayttaja::class)
    }
}
