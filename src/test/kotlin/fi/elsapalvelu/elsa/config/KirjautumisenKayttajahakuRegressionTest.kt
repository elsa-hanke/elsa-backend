package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.koulutus.OpintotietodataPersistenceService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Optional
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class KirjautumisenKayttajahakuRegressionTest {
    private val userService = mock<UserService>()
    private val userRepository = mock<UserRepository>()
    private val kutsut = mock<VerificationTokenRepository>()
    private val tallennus = mock<OpintotietodataPersistenceService>()
    private val haku = OpintotietojenTestihaku()
    private val asetukset = ApplicationProperties().apply { getStudyDataIntegrations().enabled = true }
    private val ymparisto = MockEnvironment()
    private val pyynto = MockHttpServletRequest()
    private val salaus = Cipher.getInstance("AES")
    private val avain = SecretKeySpec(ByteArray(16), "AES")
    private val kayttaja = User(id = "kayttaja", firstName = "Testi", lastName = "Henkilo")

    @BeforeEach
    fun alusta() {
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(pyynto))
        whenever(userService.findExistingUser(any(), any(), anyOrNull(), anyOrNull())).thenReturn(kayttaja)
        whenever(userRepository.findByIdWithAuthorities("kayttaja")).thenReturn(Optional.of(kayttaja))
    }

    @AfterEach
    fun tyhjennaPyynto() = RequestContextHolder.resetRequestAttributes()

    @ParameterizedTest
    @CsvSource(
        "ROLE_ERIKOISTUVA_LAAKARI, true", "ROLE_YEK_KOULUTETTAVA, true", "ROLE_KOULUTTAJA, true",
        "ROLE_VASTUUHENKILO, false", "ROLE_OPINTOHALLINNON_VIRKAILIJA, false", "ROLE_TEKNINEN_PAAKAYTTAJA, false"
    )
    fun `opintotietojen haku riippuu olemassa olevan kayttajan rooleista`(rooli: String, haetaan: Boolean) {
        kayttaja.authorities.add(Authority(rooli))

        assertThat(haeKayttaja()).isSameAs(kayttaja)

        assertThat(haku.hakukerrat).isEqualTo(if (haetaan) 1 else 0)
        if (haetaan) verify(tallennus).createOrUpdateIfChanged("kayttaja", "Testi", "Henkilo", listOfNotNull(haku.tulos))
        else verifyNoInteractions(tallennus)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_KOULUTTAJA", "ROLE_VASTUUHENKILO"])
    fun `henkilokunnan kutsu estaa opintotietohaun vaikka kayttajalla on koulutettavan rooli`(rooli: String) {
        kayttaja.authorities.add(Authority("ROLE_ERIKOISTUVA_LAAKARI"))
        val kutsuttu = User(id = "kutsuttu", authorities = mutableSetOf(Authority(rooli)))
        val kutsu = VerificationToken(id = "kutsu", user = kutsuttu)
        pyynto.setParameter("RelayState", "kutsu")
        whenever(kutsut.findById("kutsu")).thenReturn(Optional.of(kutsu))
        whenever(userRepository.findByIdWithAuthorities("kutsuttu")).thenReturn(Optional.of(kutsuttu))

        haeKayttaja()

        verify(userService).createOrUpdateUserWithToken(kutsuttu, kutsu, salaus, avain, "010101-123N", null, "Testi", "Henkilo")
        assertThat(haku.hakukerrat).isZero()
    }

    @Test
    fun `poiskytketyt integraatiot ja puuttuva hetu estavat opintotietohaun`() {
        asetukset.getStudyDataIntegrations().enabled = false
        haeKayttaja()
        asetukset.getStudyDataIntegrations().enabled = true
        haeKayttaja(hetu = null)

        assertThat(haku.hakukerrat).isZero()
        verifyNoInteractions(tallennus)
    }

    @Test
    fun `tyhja opintotietovastaus ei paivita olemassa olevaa kayttajaa`() {
        haku.tulos = null

        assertThat(haeKayttaja()).isSameAs(kayttaja)

        verifyNoInteractions(tallennus)
        verify(userRepository).findByIdWithAuthorities("kayttaja")
    }

    @Test
    fun `olemassa oleva kayttaja luetaan uudelleen opintotietojen paivityksen jalkeen`() {
        val paivitetty = User(id = "kayttaja", firstName = "Testi", lastName = "Henkilo")
        whenever(userRepository.findByIdWithAuthorities("kayttaja")).thenReturn(Optional.of(paivitetty))

        assertThat(haeKayttaja()).isSameAs(paivitetty)
        inOrder(tallennus, userRepository) {
            verify(tallennus).createOrUpdateIfChanged("kayttaja", "Testi", "Henkilo", listOfNotNull(haku.tulos))
            verify(userRepository).findByIdWithAuthorities("kayttaja")
        }
    }

    @Test
    fun `opintotietohaun tavallinen virhe ei esta olemassa olevan kayttajan hakua`() {
        haku.virhe = IllegalStateException("integraatio ei vastaa")

        assertThat(haeKayttaja()).isSameAs(kayttaja)
        verifyNoInteractions(tallennus)
    }

    @Test
    fun `tuleva opintooikeus valittyy kirjautumisvirheena`() {
        haku.virhe = Exception(LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name)

        assertThatThrownBy { haeKayttaja() }.hasMessage(LoginException.OPINTO_OIKEUS_TULEVAISUUDESSA.name)
        verifyNoInteractions(tallennus)
    }

    @ParameterizedTest
    @ValueSource(strings = ["dev", "prod"])
    fun `tuntemattoman kayttajan varaluonti sallitaan vain kehitysymparistossa`(profiili: String) {
        ymparisto.setActiveProfiles(profiili)
        whenever(userService.findExistingUser(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(null, null, kayttaja)
        if (profiili == "dev") assertThat(haeKayttaja()).isSameAs(kayttaja)
        else assertThatThrownBy { haeKayttaja() }.hasMessage(LoginException.EI_KAYTTO_OIKEUTTA.name)

        verify(tallennus).create(salaus, avain, "010101-123N", "Testi", "Henkilo", listOfNotNull(haku.tulos))
        if (profiili == "dev") verify(tallennus).createWithoutOpintotietodataOnlyDevDoNotUseInProd(
            salaus, avain, "010101-123N", "Testi", "Henkilo"
        ) else verify(tallennus, never()).createWithoutOpintotietodataOnlyDevDoNotUseInProd(any(), any(), anyOrNull(), any(), any())
    }

    @ParameterizedTest
    @CsvSource("Uusi, Nimi, true", "'', Nimi, false", "Uusi, '', false")
    fun `nimi paivitetaan vain kun molemmat nimet ovat saatavilla`(etunimi: String, sukunimi: String, tallennetaan: Boolean) {
        asetukset.getStudyDataIntegrations().enabled = false

        haeKayttaja(etunimi = etunimi, sukunimi = sukunimi)

        if (tallennetaan) {
            assertThat(kayttaja.firstName).isEqualTo(etunimi)
            assertThat(kayttaja.lastName).isEqualTo(sukunimi)
            verify(userRepository).save(kayttaja)
        } else verify(userRepository, never()).save(any())
    }

    private fun haeKayttaja(hetu: String? = "010101-123N", etunimi: String = "Testi", sukunimi: String = "Henkilo"): User {
        val palvelut = testKirjautumisenPalvelut(
            userService = userService, userRepository = userRepository, applicationProperties = asetukset,
            opintotietodataServices = listOf(haku), opintotietodataPersistenceService = tallennus,
            kirjautumisenKonteksti = KirjautumisenTestikonteksti(verificationTokenRepository = kutsut, ymparisto = ymparisto)
        )
        return palvelut.kirjautumispalvelu.haeKirjautuvaKayttaja(salaus, avain, hetu, null, etunimi, sukunimi)
    }

    private class OpintotietojenTestihaku : OpintotietodataFetchingService {
        var tulos: OpintotietodataDTO? = OpintotietodataDTO()
        var virhe: Exception? = null
        var hakukerrat = 0
        override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
            hakukerrat++
            virhe?.let { throw it }
            return tulos
        }
        override fun shouldFetchOpintotietodata() = true
        override fun getYliopisto() = YliopistoEnum.TAMPEREEN_YLIOPISTO
    }
}
