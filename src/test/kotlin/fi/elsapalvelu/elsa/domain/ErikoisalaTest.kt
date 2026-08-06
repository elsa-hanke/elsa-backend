package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
class ErikoisalaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Erikoisala::class)
    }
}
