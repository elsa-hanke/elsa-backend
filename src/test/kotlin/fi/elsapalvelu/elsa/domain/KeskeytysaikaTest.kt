package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
class KeskeytysaikaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Keskeytysaika::class)
    }
}
