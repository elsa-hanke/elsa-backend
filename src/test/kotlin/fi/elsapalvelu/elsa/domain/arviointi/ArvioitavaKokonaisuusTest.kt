package fi.elsapalvelu.elsa.domain.arviointi

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class ArvioitavaKokonaisuusTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(ArvioitavaKokonaisuus::class)
    }
}
