package fi.elsapalvelu.elsa.domain.koulutus

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class KoulutussuunnitelmaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(Koulutussuunnitelma::class)
    }
}
