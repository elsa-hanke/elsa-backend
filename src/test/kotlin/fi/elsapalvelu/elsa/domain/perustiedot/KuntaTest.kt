package fi.elsapalvelu.elsa.domain.perustiedot

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class KuntaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Kunta::class)
    }
}
