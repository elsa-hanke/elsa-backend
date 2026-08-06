package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.domain.suoritteet.Suoritemerkinta
class SuoritemerkintaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Suoritemerkinta::class)
    }
}
