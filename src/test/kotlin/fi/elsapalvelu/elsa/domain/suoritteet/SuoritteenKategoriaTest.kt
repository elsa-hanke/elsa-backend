package fi.elsapalvelu.elsa.domain.suoritteet

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test

class SuoritteenKategoriaTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(SuoritteenKategoria::class)
    }
}
