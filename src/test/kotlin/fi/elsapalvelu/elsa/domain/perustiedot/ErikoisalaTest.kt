package fi.elsapalvelu.elsa.domain.perustiedot

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class ErikoisalaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Erikoisala::class)
    }
}
