package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
class ArvioitavaKokonaisuusTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(ArvioitavaKokonaisuus::class)
    }
}
