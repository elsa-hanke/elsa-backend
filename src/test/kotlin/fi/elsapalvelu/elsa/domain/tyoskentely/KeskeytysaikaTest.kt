package fi.elsapalvelu.elsa.domain.tyoskentely

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class KeskeytysaikaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Keskeytysaika::class)
    }
}
