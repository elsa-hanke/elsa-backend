package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Clock

class TokeninVoimassaolonKelloTest {
    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `token kelpaa vain ennen tarkkaa vanhenemishetkea`(aika: KellonTestitapaus) {
        val kello = mock<Clock>()
        whenever(kello.zone).thenReturn(aika.kello().zone)
        whenever(kello.instant()).thenReturn(aika.aikaleima())
        val valimuisti = AuthenticationTokenCache(kello)
        val token = AuthenticationToken("token", aika.paikallinenAika().plusSeconds(60))
        valimuisti.storeTokenByClientId("asiakas", token)

        assertThat(valimuisti.getTokenByClientId("asiakas")).isSameAs(token)
        whenever(kello.instant()).thenReturn(aika.aikaleima().plusSeconds(59))
        assertThat(valimuisti.getTokenByClientId("asiakas")).isSameAs(token)
        whenever(kello.instant()).thenReturn(aika.aikaleima().plusSeconds(60))
        assertThat(valimuisti.getTokenByClientId("asiakas")).isNull()
        whenever(kello.instant()).thenReturn(aika.aikaleima().plusSeconds(61))
        assertThat(valimuisti.getTokenByClientId("asiakas")).isNull()
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `eri asiakkaiden tokenit eivat korvaa toisiaan`(aika: KellonTestitapaus) {
        val valimuisti = AuthenticationTokenCache(aika.kello())
        val token = AuthenticationToken("ensimmainen", aika.paikallinenAika().plusDays(1))
        val toinen = AuthenticationToken("toinen", aika.paikallinenAika().plusDays(1))
        valimuisti.storeTokenByClientId("a", token)
        valimuisti.storeTokenByClientId("b", toinen)

        assertThat(valimuisti.getTokenByClientId("a")).isSameAs(token)
        assertThat(valimuisti.getTokenByClientId("b")).isSameAs(toinen)
        assertThat(valimuisti.getTokenByClientId("tuntematon")).isNull()
    }
}
