package fi.elsapalvelu.elsa.domain.arviointi

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class SuoritusarvioinninKommenttiTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(SuoritusarvioinninKommentti::class)
    }
}
