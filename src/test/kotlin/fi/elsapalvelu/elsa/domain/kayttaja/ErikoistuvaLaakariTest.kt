package fi.elsapalvelu.elsa.domain.kayttaja

import fi.elsapalvelu.elsa.web.rest.entityEqualsVerifier as verifyEntityEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class ErikoistuvaLaakariTest {

    @Test
    fun equalsVerifier() {
        verifyEntityEquals(ErikoistuvaLaakari::class)
    }

    @Test
    fun `get opintooikeus kaytossa returns null when no oikeus is selected`() {
        val erikoistuvaLaakari = ErikoistuvaLaakari(
            opintooikeudet = mutableSetOf(Opintooikeus(kaytossa = false))
        )

        assertNull(erikoistuvaLaakari.getOpintooikeusKaytossa())
        assertNull(erikoistuvaLaakari.getYliopistoNimi())
    }
}
