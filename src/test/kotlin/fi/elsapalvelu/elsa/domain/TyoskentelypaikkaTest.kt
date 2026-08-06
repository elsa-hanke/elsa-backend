package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelypaikka
class TyoskentelypaikkaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Tyoskentelypaikka::class)
    }
}
