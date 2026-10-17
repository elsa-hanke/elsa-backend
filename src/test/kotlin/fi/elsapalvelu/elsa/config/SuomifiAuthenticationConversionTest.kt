package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
import java.util.Base64
import java.util.Optional
import javax.xml.namespace.QName

class SuomifiAuthenticationConversionTest {
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
        verificationTokenRepository = verificationTokenRepository
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

        assertThatThrownBy { configuration.convertAuthentication(responseToken()) }
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

        val authentication = configuration.convertAuthentication(responseToken())

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

        configuration.convertAuthentication(responseToken())

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

    private fun existingUser(user: User, state: KayttajatilinTila): Kayttaja {
        val kayttaja = Kayttaja(tila = state, user = user)
        whenever(userService.findExistingUser(any(), any(), eq("010101-123N"), isNull())).thenReturn(user)
        whenever(kayttajaRepository.findOneByUserIdWithAuthorities("user-123"))
            .thenReturn(Optional.of(kayttaja))
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

    private fun responseToken(): OpenSaml5AuthenticationProvider.ResponseToken {
        val registration = RelyingPartyRegistration.withRegistrationId("suomifi")
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
                attributes.add(attribute("urn:oid:1.2.246.21", "010101-123N"))
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
