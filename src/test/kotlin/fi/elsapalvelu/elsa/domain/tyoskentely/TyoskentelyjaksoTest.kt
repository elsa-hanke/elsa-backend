package fi.elsapalvelu.elsa.domain.tyoskentely

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class TyoskentelyjaksoTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Tyoskentelyjakso::class)
    }
}
