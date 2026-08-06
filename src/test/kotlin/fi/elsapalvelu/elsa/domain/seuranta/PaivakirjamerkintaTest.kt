package fi.elsapalvelu.elsa.domain.seuranta

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class PaivakirjamerkintaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Paivakirjamerkinta::class)
    }
}
