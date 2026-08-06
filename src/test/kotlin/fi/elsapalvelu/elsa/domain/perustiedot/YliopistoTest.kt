package fi.elsapalvelu.elsa.domain.perustiedot

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class YliopistoTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Yliopisto::class)
    }
}
