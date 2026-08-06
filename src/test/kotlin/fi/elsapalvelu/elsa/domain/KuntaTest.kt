package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.domain.perustiedot.Kunta
class KuntaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Kunta::class)
    }
}
