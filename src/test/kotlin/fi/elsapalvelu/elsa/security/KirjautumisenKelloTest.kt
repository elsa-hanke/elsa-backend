package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.LoginException
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen.Kirjautumiskutsupalvelu
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class KirjautumisenKelloTest {
    @AfterEach
    fun tyhjennaKontekstit() {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `nimiristiriidan viesti vanhenee viiden minuutin kuluttua kellon hetkesta`(aika: KellonTestitapaus) {
        val kutsut = mock<VerificationTokenRepository>()
        val tunnukset = mock<UserRepository>()
        val kayttajat = mock<UserService>()
        val kutsuttu = User(id = "kutsuttu", firstName = "Kutsuttu", lastName = "Henkilö")
        whenever(kutsut.findById("kutsu")).thenReturn(Optional.of(VerificationToken(user = kutsuttu)))
        whenever(tunnukset.findByIdWithAuthorities("kutsuttu")).thenReturn(Optional.of(kutsuttu))
        whenever(kayttajat.createOrUpdateUserWithToken(any(), any(), any(), any(), anyOrNull(), anyOrNull(), any(), any()))
            .thenThrow(IllegalArgumentException(LoginException.VIRHEELLINEN_NIMI.name))
        val pyynto = MockHttpServletRequest().apply { setParameter("RelayState", "kutsu") }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(pyynto))
        val palvelu = Kirjautumiskutsupalvelu(kutsut, tunnukset, kayttajat, aika.kello())

        assertThrows<IllegalArgumentException> { palvelu.kasitteleKutsu(mock(), mock(), null, null, "Toinen", "Henkilö") }

        val viesti = pyynto.session.getAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE) as LoginNameMismatch
        assertThat(viesti.expiresAt).isEqualTo(aika.aikaleima().plusSeconds(300))
        assertThat(viesti.invitationName).isEqualTo("Kutsuttu Henkilö")
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `toisen kayttajan nakyma sallitaan viela valtuutuksen viimeisena paivana`(aika: KellonTestitapaus) {
        val tunnukset = mock<UserRepository>()
        val oikeudet = mock<OpintooikeusRepository>()
        val kayttajat = mock<KayttajaRepository>()
        val valtuutukset = mock<KouluttajavaltuutusRepository>()
        val kouluttaja = User(id = "kouluttaja", authorities = mutableSetOf(Authority(KOULUTTAJA)))
        val erikoistuja = User(id = "erikoistuja", firstName = "Testi", lastName = "Lääkäri")
        val opintooikeus = Opintooikeus(id = 11L, erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = Kayttaja(user = erikoistuja)))
        whenever(tunnukset.findByIdWithAuthorities("kouluttaja")).thenReturn(Optional.of(kouluttaja))
        whenever(oikeudet.findById(11L)).thenReturn(Optional.of(opintooikeus))
        whenever(kayttajat.findOneByUserIdWithErikoisalat("kouluttaja")).thenReturn(Optional.of(Kayttaja(user = kouluttaja)))
        whenever(valtuutukset.findByValtuuttajaOpintooikeusIdAndValtuutettuUserIdAndPaattymispaivaAfter(11L, "kouluttaja", aika.paiva.minusDays(1)))
            .thenReturn(Optional.of(Kouluttajavaltuutus()))
        SecurityContextHolder.getContext().authentication = Saml2Authentication(testSamlPrincipal("kouluttaja", emptyMap()), "test", listOf(SimpleGrantedAuthority(KOULUTTAJA)))
        val pyynto = MockHttpServletRequest("GET", "/api/login/impersonate").apply { setParameter("opintooikeusId", "11") }
        val suodatin = ElsaSwitchUserFilter(oikeudet, kayttajat, tunnukset, valtuutukset, aika.kello())

        suodatin.doFilter(pyynto, MockHttpServletResponse(), MockFilterChain())

        verify(valtuutukset).findByValtuuttajaOpintooikeusIdAndValtuutettuUserIdAndPaattymispaivaAfter(11L, "kouluttaja", aika.paiva.minusDays(1))
        assertThat(SecurityContextHolder.getContext().authentication!!.name).isEqualTo("erikoistuja")
    }
}
