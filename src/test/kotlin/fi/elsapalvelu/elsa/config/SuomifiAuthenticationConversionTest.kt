package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.OpintoopasRepository
import fi.elsapalvelu.elsa.service.impl.kayttaja.OpintooikeusServiceImpl
import fi.elsapalvelu.elsa.service.mapper.kayttaja.OpintooikeusMapper
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.koulutus.OpintotietodataPersistenceService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.schema.XSString
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.AttributeStatement
import org.opensaml.saml.saml2.core.AttributeValue
import org.opensaml.saml.saml2.core.NameID
import org.opensaml.saml.saml2.core.NameIDType
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.core.Subject
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.saml2.core.OpenSamlInitializationService
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Clock
import java.util.Base64
import java.util.Optional
import javax.xml.namespace.QName

class SuomifiAuthenticationConversionTest {
    private val opintooikeusRepository = mock(OpintooikeusRepository::class.java)
    private val userService = mock(UserService::class.java)
    private val opintooikeusService = mock(OpintooikeusService::class.java)
    private val kayttajaRepository = mock(KayttajaRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val verificationTokenRepository = mock(VerificationTokenRepository::class.java)
    private val properties = ApplicationProperties().apply {
        getStudyDataIntegrations().enabled = false
        getSecurity().encodedKey = Base64.getEncoder().encodeToString(ByteArray(16))
        getSecurity().secretKeyAlgorithm = "AES"
        getSecurity().cipherAlgorithm = "AES"
    }
    private val configuration = testSecurityConfiguration(
        applicationProperties = properties,
        userService = userService,
        opintooikeusService = opintooikeusService,
        kayttajaRepository = kayttajaRepository,
        userRepository = userRepository,
        kirjautumisenRepositories = KirjautumisenTestRepositories(verificationTokenRepository, opintooikeusRepository)
    )

    @BeforeEach
    fun setUp() {
        OpenSamlInitializationService.initialize()
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest()))
    }

    @AfterEach
    fun clearRequestContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun activeExistingSuomifiUserGetsOnlyTheirPersistedAuthorities() {
        val user = userWithRole(VASTUUHENKILO)
        existingUser(user, KayttajatilinTila.AKTIIVINEN)

        val authentication = configuration.convertAuthentication(responseToken())

        assertThat(authentication.name).isEqualTo("user-123")
        assertThat(authentication.authorities.map { it.authority }).containsExactly(VASTUUHENKILO)
        assertThat(authentication.principal).extracting("relyingPartyRegistrationId").isEqualTo("suomifi")
    }

    @Suppress("DEPRECATION", "Deprecation")
    @Test
    fun suomifiLoginPreservesExternalNameIdForLogout() {
        existingUser(userWithRole(VASTUUHENKILO), KayttajatilinTila.AKTIIVINEN)

        val principal = configuration.convertAuthentication(responseToken()).principal as Saml2AuthenticatedPrincipal

        assertThat(principal.attributes["nameID"]).containsExactly("external-subject")
        assertThat(principal.attributes["nameIDFormat"]).containsExactly(NameIDType.PERSISTENT)
        assertThat(principal.attributes["nameIDQualifier"]).containsExactly("https://idp.example")
        assertThat(principal.attributes["nameIDSPQualifier"]).containsExactly("https://elsapalvelu.fi")
    }

    @Test
    fun unknownSuomifiUserCannotLogIn() {
        assertThatThrownBy { configuration.convertAuthentication(responseToken()) }
            .hasMessage(LoginException.EI_KAYTTO_OIKEUTTA.name)
    }

    @Test
    fun passiveSuomifiAccountCannotLogIn() {
        existingUser(userWithRole(VASTUUHENKILO), KayttajatilinTila.PASSIIVINEN)

        assertThatThrownBy { configuration.convertAuthentication(responseToken()) }
            .hasMessage(LoginException.TILI_PASSIVOITU.name)
    }

    @Test
    fun traineeWithoutCurrentStudyRightCannotLogIn() {
        existingUser(userWithRole(ERIKOISTUVA_LAAKARI), KayttajatilinTila.AKTIIVINEN)

        whenever(opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId("user-123")).thenReturn(listOf(Opintooikeus()))
        assertThatThrownBy { configurationWithOpintooikeusService().convertAuthentication(responseToken()) }
            .hasMessage(LoginException.EI_OPINTO_OIKEUTTA.name)
    }

    @Test
    fun invitedResponsiblePersonIsActivatedOnSuomifiLogin() {
        val kayttaja = existingUser(userWithRole(VASTUUHENKILO), KayttajatilinTila.KUTSUTTU)

        configuration.convertAuthentication(responseToken())

        assertThat(kayttaja.tila).isEqualTo(KayttajatilinTila.AKTIIVINEN)
        verify(kayttajaRepository).save(kayttaja)
    }

    @Test
    fun traineeWithoutStudyRightCanContinueAsTrainer() {
        val user = userWithRole(ERIKOISTUVA_LAAKARI).apply {
            authorities.add(Authority(name = KOULUTTAJA))
        }
        existingUser(user, KayttajatilinTila.AKTIIVINEN)

        val authentication = configurationWithOpintooikeusService().convertAuthentication(responseToken())

        assertThat(authentication.authorities.map { it.authority }).containsExactly(KOULUTTAJA)
        assertThat(user.activeAuthority?.name).isEqualTo(KOULUTTAJA)
        verify(userRepository).save(user)
    }

    @Test
    fun suomifiLoginUpdatesChangedName() {
        val user = userWithRole(VASTUUHENKILO).apply { firstName = "Old" }
        existingUser(user, KayttajatilinTila.AKTIIVINEN)

        configuration.convertAuthentication(responseToken())

        assertThat(user.firstName).isEqualTo("Test")
        verify(userRepository).save(user)
    }

    @Test
    fun suomifiLoginSelectsAuthorityWhenNoneIsActive() {
        val user = userWithRole(VASTUUHENKILO).apply { activeAuthority = null }
        existingUser(user, KayttajatilinTila.AKTIIVINEN)

        configurationWithOpintooikeusService().convertAuthentication(responseToken())

        assertThat(user.activeAuthority?.name).isEqualTo(VASTUUHENKILO)
        verify(userRepository).save(user)
    }

    @Test
    fun suomifiInvitationRelayStateLinksTokenBeforeLogin() {
        val user = userWithRole(VASTUUHENKILO)
        val invitation = VerificationToken(id = "invitation-123", user = user)
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        whenever(verificationTokenRepository.findById("invitation-123")).thenReturn(Optional.of(invitation))
        whenever(userRepository.findByIdWithAuthorities("user-123")).thenReturn(Optional.of(user))
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest().apply {
            addParameter("RelayState", "invitation-123")
        }))

        configuration.convertAuthentication(responseToken())

        verify(userService).createOrUpdateUserWithToken(
            eq(user), eq(invitation), any(), any(), eq("010101-123N"), isNull(), eq("Test"), eq("User")
        )
    }

    @Test
    fun firstSuomifiLoginPersistsFetchedStudyDataBeforeGrantingAccess() {
        val user = userWithRole(VASTUUHENKILO)
        val studyData = OpintotietodataDTO()
        val fetchingService = object : OpintotietodataFetchingService {
            override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? = studyData
            override fun shouldFetchOpintotietodata() = true
            override fun getYliopisto() = YliopistoEnum.TAMPEREEN_YLIOPISTO
        }
        val persistenceService = mock(OpintotietodataPersistenceService::class.java)
        properties.getStudyDataIntegrations().enabled = true
        whenever(userService.findExistingUser(any(), any(), eq("010101-123N"), isNull()))
            .thenReturn(null, user)
        whenever(kayttajaRepository.findOneByUserIdWithAuthorities("user-123"))
            .thenReturn(Optional.of(Kayttaja(tila = KayttajatilinTila.AKTIIVINEN, user = user)))
        val firstLoginConfiguration = testSecurityConfiguration(
            applicationProperties = properties,
            userService = userService,
            opintotietodataServices = listOf(fetchingService),
            opintotietodataPersistenceService = persistenceService,
            kayttajaRepository = kayttajaRepository
        )

        val authentication = firstLoginConfiguration.convertAuthentication(responseToken())

        assertThat(authentication.name).isEqualTo("user-123")
        verify(persistenceService).create(any(), any(), eq("010101-123N"), eq("Test"), eq("User"), eq(listOf(studyData)))
    }

    @ParameterizedTest
    @CsvSource(
        "ROLE_YEK_KOULUTETTAVA, true",
        "ROLE_YEK_KOULUTETTAVA, false",
        "ROLE_ERIKOISTUVA_LAAKARI, true",
        "ROLE_ERIKOISTUVA_LAAKARI, false"
    )
    fun kouluttajanKirjautuminenPalauttaaPuuttuvanRoolin(role: String, integrationsEnabled: Boolean) {
        val user = userWithRole(KOULUTTAJA)
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        properties.getStudyDataIntegrations().enabled = integrationsEnabled
        whenever(userRepository.findByIdWithAuthorities(user.id!!)).thenReturn(Optional.of(user))
        doAnswer {
            user.authorities.add(Authority(role))
            null
        }.whenever(opintooikeusService).checkOpintooikeusAndRoles(user)

        val authentication = configuration.convertAuthentication(responseToken())

        verify(opintooikeusService).checkOpintooikeusAndRoles(user)
        assertThat(authentication.authorities.map { it.authority }).containsExactlyInAnyOrder(KOULUTTAJA, role)
        assertThat(user.activeAuthority?.name).isEqualTo(KOULUTTAJA)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_YEK_KOULUTETTAVA", ERIKOISTUVA_LAAKARI])
    fun hakaKirjautuminenPalauttaaKouluttajanRoolitIlmanHetua(role: String) {
        val user = userWithRole(KOULUTTAJA)
        properties.getStudyDataIntegrations().enabled = true
        whenever(userService.findExistingUser(any(), any(), isNull(), eq("trainer@tuni.fi"))).thenReturn(user)
        whenever(kayttajaRepository.findOneByUserIdWithAuthorities(user.id!!))
            .thenReturn(Optional.of(Kayttaja(tila = KayttajatilinTila.AKTIIVINEN, user = user)))
        doAnswer {
            user.authorities.add(Authority(role))
            null
        }.whenever(opintooikeusService).checkOpintooikeusAndRoles(user)

        val authentication = configuration.convertAuthentication(responseToken(haka = true))

        verify(opintooikeusService).checkOpintooikeusAndRoles(user)
        assertThat(authentication.authorities.map { it.authority }).containsExactlyInAnyOrder(KOULUTTAJA, role)
        assertThat(user.activeAuthority?.name).isEqualTo(KOULUTTAJA)
        assertThat(authentication.principal).extracting("relyingPartyRegistrationId").isEqualTo("haka-tampere")
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun kouluttajanKirjautuminenIlmanOpintooikeutta(integrationsEnabled: Boolean) {
        val user = userWithRole(KOULUTTAJA)
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        properties.getStudyDataIntegrations().enabled = integrationsEnabled
        whenever(userRepository.findByIdWithAuthorities(user.id!!)).thenReturn(Optional.of(user))

        val authentication = configurationWithOpintooikeusService().convertAuthentication(responseToken())

        verify(userRepository, never()).save(any())
        assertThat(authentication.authorities.map { it.authority }).containsExactly(KOULUTTAJA)
        assertThat(user.activeAuthority?.name).isEqualTo(KOULUTTAJA)
    }

    @ParameterizedTest
    @CsvSource(
        "ROLE_ERIKOISTUVA_LAAKARI, false, false",
        "ROLE_ERIKOISTUVA_LAAKARI, false, true",
        "ROLE_ERIKOISTUVA_LAAKARI, true, false",
        "ROLE_ERIKOISTUVA_LAAKARI, true, true",
        "ROLE_YEK_KOULUTETTAVA, false, false",
        "ROLE_YEK_KOULUTETTAVA, false, true",
        "ROLE_YEK_KOULUTETTAVA, true, false",
        "ROLE_YEK_KOULUTETTAVA, true, true"
    )
    fun paattynytOpintooikeusEstaaKirjautumisenIlmanMuitaRooleja(role: String, haka: Boolean, integrationsEnabled: Boolean) {
        val user = userWithRole(role)
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        properties.getStudyDataIntegrations().enabled = integrationsEnabled
        whenever(userRepository.findByIdWithAuthorities(user.id!!)).thenReturn(Optional.of(user))
        if (haka) {
            whenever(userService.findExistingUser(any(), any(), isNull(), eq("trainer@tuni.fi"))).thenReturn(user)
        }

        whenever(opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId("user-123")).thenReturn(listOf(Opintooikeus()))
        assertThatThrownBy { configurationWithOpintooikeusService().convertAuthentication(responseToken(haka)) }
            .hasMessage(LoginException.EI_OPINTO_OIKEUTTA.name)
        assertThat(user.authorities).isEmpty()
        assertThat(user.activeAuthority).isNull()
        verify(userRepository).save(user)
    }

    @ParameterizedTest
    @ValueSource(strings = [KOULUTTAJA, VASTUUHENKILO, "ROLE_OPINTOHALLINNON_VIRKAILIJA", "ROLE_TEKNINEN_PAAKAYTTAJA"])
    fun henkilokunnanKirjautuminenTarkistaaOpintooikeudet(role: String) {
        val user = userWithRole(role)
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        doAnswer {
            user.authorities.add(Authority("ROLE_YEK_KOULUTETTAVA"))
            null
        }.whenever(opintooikeusService).checkOpintooikeusAndRoles(user)

        val authentication = configuration.convertAuthentication(responseToken())

        verify(opintooikeusService).checkOpintooikeusAndRoles(user)
        assertThat(authentication.authorities.map { it.authority })
            .containsExactlyInAnyOrder(role, "ROLE_YEK_KOULUTETTAVA")
        assertThat(user.activeAuthority?.name).isEqualTo(role)
    }

    @ParameterizedTest
    @CsvSource(
        "ROLE_ERIKOISTUVA_LAAKARI, false", "ROLE_ERIKOISTUVA_LAAKARI, true",
        "ROLE_YEK_KOULUTETTAVA, false", "ROLE_YEK_KOULUTETTAVA, true"
    )
    fun kirjautuminenOnnistuuSamanOpintooikeudenUusimisenJalkeen(role: String, haka: Boolean) {
        val user = userWithRole(role)
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        if (haka) {
            whenever(userService.findExistingUser(any(), any(), isNull(), eq("trainer@tuni.fi"))).thenReturn(user)
        }
        val right = Opintooikeus(id = 1L, kaytossa = true,
            erikoisala = Erikoisala(id = if (role == ERIKOISTUVA_LAAKARI) 50L else 61L))
        whenever(opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId("user-123")).thenReturn(listOf(right))
        val login = configurationWithOpintooikeusService()

        assertThatThrownBy { login.convertAuthentication(responseToken(haka)) }
            .hasMessage(LoginException.EI_OPINTO_OIKEUTTA.name)
        assertThat(user.authorities).isEmpty()

        // Import renews the existing right; it does not create a right or restore its role itself.
        whenever(opintooikeusRepository.findAllValidByErikoistuvaLaakariKayttajaUserId("user-123")).thenReturn(listOf(right))
        val authentication = login.convertAuthentication(responseToken(haka))

        assertThat(authentication.authorities.map { it.authority }).containsExactly(role)
        assertThat(user.activeAuthority?.name).isEqualTo(role)
        assertThat(right.id).isEqualTo(1L)
    }

    @Test
    fun kayttajaIlmanRoolejaJaOpintooikeuksiaEiVoiKirjautua() {
        val user = userWithRole(KOULUTTAJA).apply {
            authorities.clear()
            activeAuthority = null
        }
        existingUser(user, KayttajatilinTila.AKTIIVINEN)

        assertThatThrownBy { configurationWithOpintooikeusService().convertAuthentication(responseToken()) }
            .hasMessage(LoginException.EI_KAYTTO_OIKEUTTA.name)
    }

    @ParameterizedTest
    @CsvSource(
        "ROLE_KOULUTTAJA, false", "ROLE_KOULUTTAJA, true",
        "ROLE_VASTUUHENKILO, false", "ROLE_VASTUUHENKILO, true",
        "ROLE_OPINTOHALLINNON_VIRKAILIJA, false", "ROLE_OPINTOHALLINNON_VIRKAILIJA, true",
        "ROLE_TEKNINEN_PAAKAYTTAJA, false", "ROLE_TEKNINEN_PAAKAYTTAJA, true"
    )
    fun henkilokunnanKirjautuminenOnnistuuTarkistusvirheestaHuolimatta(role: String, mixedRoles: Boolean) {
        val user = userWithRole(role)
        if (mixedRoles) {
            user.authorities.add(Authority(ERIKOISTUVA_LAAKARI))
            user.authorities.add(Authority("ROLE_YEK_KOULUTETTAVA"))
            user.activeAuthority = Authority("ROLE_YEK_KOULUTETTAVA")
        }
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        doAnswer {
            // Even a partially executed reconciliation cannot grant study access in the fallback session.
            user.authorities.add(Authority("ROLE_YEK_KOULUTETTAVA"))
            error("study service unavailable")
        }.whenever(opintooikeusService).checkOpintooikeusAndRoles(user)

        val authentication = configuration.convertAuthentication(responseToken())

        assertThat(authentication.authorities.map { it.authority }).containsExactly(role)
        verify(userRepository, never()).save(any())
        if (mixedRoles) {
            verify(userService).updateRooli(role, user.id!!)
        } else {
            verify(userService, never()).updateRooli(any(), any())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [ERIKOISTUVA_LAAKARI, "ROLE_YEK_KOULUTETTAVA", ""])
    fun tarkistusvirheEiSalliKirjautumistaPelkillaKoulutettavanRooleillaTaiIlmanRooleja(role: String) {
        val user = userWithRole(role)
        if (role.isEmpty()) user.authorities.clear()
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        doAnswer { error("study service unavailable") }
            .whenever(opintooikeusService).checkOpintooikeusAndRoles(user)

        assertThatThrownBy { configuration.convertAuthentication(responseToken()) }
            .hasMessage("study service unavailable")
    }

    @Test
    fun henkilokunnanRoolinAktivointivirheEiSalliKirjautumistaVaarallaAktiivisellaRoolilla() {
        val user = userWithRole(KOULUTTAJA).apply { activeAuthority = Authority("ROLE_YEK_KOULUTETTAVA") }
        existingUser(user, KayttajatilinTila.AKTIIVINEN)
        doAnswer { error("opintooikeuksien tarkistus epäonnistui") }
            .whenever(opintooikeusService).checkOpintooikeusAndRoles(user)
        doAnswer { error("roolin aktivointi epäonnistui") }
            .whenever(userService).updateRooli(KOULUTTAJA, user.id!!)

        assertThatThrownBy { configuration.convertAuthentication(responseToken()) }
            .hasMessage("roolin aktivointi epäonnistui")
    }

    private fun configurationWithOpintooikeusService(): SecurityConfiguration {
        val service = OpintooikeusServiceImpl(
            opintooikeusRepository, mock(OpintooikeusMapper::class.java), mock(ErikoistuvaLaakariRepository::class.java),
            Clock.systemUTC(), mock(OpintoopasRepository::class.java), userRepository
        )
        return testSecurityConfiguration(
            applicationProperties = properties, userService = userService, opintooikeusService = service,
            kayttajaRepository = kayttajaRepository, userRepository = userRepository,
            kirjautumisenRepositories = KirjautumisenTestRepositories(opintooikeusRepository = opintooikeusRepository)
        )
    }

    private fun existingUser(user: User, state: KayttajatilinTila): Kayttaja {
        val kayttaja = Kayttaja(tila = state, user = user)
        whenever(userService.findExistingUser(any(), any(), eq("010101-123N"), isNull())).thenReturn(user)
        whenever(kayttajaRepository.findOneByUserIdWithAuthorities("user-123"))
            .thenReturn(Optional.of(kayttaja))
        whenever(userRepository.findByIdWithAuthorities("user-123")).thenReturn(Optional.of(user))
        return kayttaja
    }

    private fun userWithRole(role: String): User {
        val authority = Authority(name = role)
        return User(
            id = "user-123",
            firstName = "Test",
            lastName = "User",
            authorities = mutableSetOf(authority),
            activeAuthority = authority
        )
    }

    private fun responseToken(haka: Boolean = false): OpenSaml5AuthenticationProvider.ResponseToken {
        val registrationId = if (haka) "haka-tampere" else "suomifi"
        val registration = RelyingPartyRegistration.withRegistrationId(registrationId)
            .entityId("https://elsapalvelu.fi/saml/suomifi")
            .assertionConsumerServiceLocation("https://elsapalvelu.fi/login/saml2/sso/suomifi")
            .assertingPartyMetadata {
                it.entityId("https://idp.example")
                    .singleSignOnServiceLocation("https://idp.example/login")
            }.build()
        val assertion = saml<Assertion>(Assertion.DEFAULT_ELEMENT_NAME).apply {
            subject = saml<Subject>(Subject.DEFAULT_ELEMENT_NAME).apply {
                nameID = saml<NameID>(NameID.DEFAULT_ELEMENT_NAME).apply {
                    value = "external-subject"
                    format = NameIDType.PERSISTENT
                    nameQualifier = "https://idp.example"
                    spNameQualifier = "https://elsapalvelu.fi"
                }
            }
            attributeStatements.add(saml<AttributeStatement>(AttributeStatement.DEFAULT_ELEMENT_NAME).apply {
                attributes.add(attribute("urn:oid:2.5.4.42", "Test"))
                attributes.add(attribute("urn:oid:2.5.4.4", "User"))
                if (haka) {
                    attributes.add(attribute("urn:oid:1.3.6.1.4.1.5923.1.1.1.6", "trainer@tuni.fi"))
                } else {
                    attributes.add(attribute("urn:oid:1.2.246.21", "010101-123N"))
                }
            })
        }
        val response = saml<Response>(Response.DEFAULT_ELEMENT_NAME).apply { assertions.add(assertion) }
        val constructor = OpenSaml5AuthenticationProvider.ResponseToken::class.java
            .getDeclaredConstructor(Response::class.java, Saml2AuthenticationToken::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(response, Saml2AuthenticationToken(registration, "response"))
    }

    private fun attribute(name: String, value: String): Attribute =
        saml<Attribute>(Attribute.DEFAULT_ELEMENT_NAME).apply {
            this.name = name
            val xmlValue = XMLObjectProviderRegistrySupport.getBuilderFactory()
                .ensureBuilder<XMLObject>(XSString.TYPE_NAME)
                .buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME) as XSString
            xmlValue.value = value
            attributeValues.add(xmlValue)
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : XMLObject> saml(element: QName): T =
        XMLObjectProviderRegistrySupport.getBuilderFactory().ensureBuilder<XMLObject>(element).buildObject(element) as T
}
