package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.audit.AuditLoggingWrapper
import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.*
import org.opensaml.saml.common.assertion.ValidationContext
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters
import org.opensaml.saml.saml2.core.Assertion
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver.AuthnRequestContext
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutResponseResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutResponseResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.filter.CorsFilter
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_DEVELOPMENT
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_PRODUCTION
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Jos relay state jää tyhjäksi, redirect strategialla allekirjoitettuihin SAML pyyntöihin
 * tulee ylimääräinen query parametri, joka sekoittaa allekirjoituksen tarkistuksen.
 * Kierretään lisäämällä oma vakio parametri jos pyynnössä ei ole mukana relay statea.
 * @see OpenSaml4AuthenticationRequestResolver
 */
private const val DEFAULT_RELAY_STATE = "elsa"

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfiguration(
    private val corsFilter: CorsFilter,
    private val applicationProperties: ApplicationProperties,
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService,
    private val opintotietodataFetchingServices: List<OpintotietodataFetchingService>,
    private val opintotietodataPersistenceService: OpintotietodataPersistenceService,
    private val opintosuorituksetFetchingService: List<OpintosuorituksetFetchingService>,
    private val opintosuorituksetPersistenceService: OpintosuorituksetPersistenceService,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val env: Environment,
    private val applicationContext: ApplicationContext
) {

    private val log = LoggerFactory.getLogger(SecurityConfiguration::class.java)

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        val withHttpOnlyFalse = CookieCsrfTokenRepository.withHttpOnlyFalse()
        val authenticationProvider = OpenSaml4AuthenticationProvider()
        authenticationProvider.setAssertionValidator(
            OpenSaml4AuthenticationProvider.createDefaultAssertionValidator {
                val relyingPartyRegistration: RelyingPartyRegistration =
                    it.token.relyingPartyRegistration
                val audience = relyingPartyRegistration.entityId
                val recipient = relyingPartyRegistration.assertionConsumerServiceLocation
                val assertingPartyEntityId =
                    relyingPartyRegistration.assertingPartyDetails.entityId
                val params: MutableMap<String, Any> = HashMap()
                if (assertionContainsInResponseTo(it.assertion)) {
                    val requestId = it.token.authenticationRequest.id
                    params[SAML2AssertionValidationParameters.SC_VALID_IN_RESPONSE_TO] = requestId
                }
                params[SAML2AssertionValidationParameters.COND_VALID_AUDIENCES] =
                    setOf(
                        if (audience.contains("haka")) audience.substring(
                            0,
                            audience.indexOf("haka")
                        ) + "haka"; else audience
                    )
                params[SAML2AssertionValidationParameters.SC_VALID_RECIPIENTS] =
                    setOf(recipient)
                params[SAML2AssertionValidationParameters.VALID_ISSUERS] =
                    setOf(assertingPartyEntityId)
                ValidationContext(params)
            }
        )
        authenticationProvider.setResponseAuthenticationConverter(authenticationConverter())
        withHttpOnlyFalse.setCookieDomain(applicationProperties.getCsrf().cookie.domain)
        val requestHandler = CsrfTokenRequestAttributeHandler()
        requestHandler.setCsrfRequestAttributeName(null);

        val httpConfiguration = http
            .csrf { csrf ->
                csrf
                    .csrfTokenRequestHandler(requestHandler)
                    .csrfTokenRepository(withHttpOnlyFalse)
                    .ignoringRequestMatchers("/api/logout")
            }
            .addFilterBefore(corsFilter, CsrfFilter::class.java)
            .addFilterAfter(
                ElsaSwitchUserFilter(
                    opintooikeusRepository,
                    kayttajaRepository,
                    userRepository,
                    kouluttajavaltuutusRepository
                ),
                AuthorizationFilter::class.java
            )
            .exceptionHandling()
            .accessDeniedHandler { request, response, _ ->
                AuditLoggingWrapper.warn(
                    "Access denied for " +
                        "user: ${request.let { it?.userPrincipal?.name }}, " +
                        "method: ${request.method}, " +
                        "path: ${request.requestURI}}, " +
                        "ip: ${request.getHeader("X-Forwarded-For")}"
                )
                response.sendError(HttpServletResponse.SC_FORBIDDEN)
            }
            .authenticationEntryPoint { _, response, _ ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            }
            .and()
            .headers()
            .httpStrictTransportSecurity()
            // 12 months
            .maxAgeInSeconds(31536000)
            .includeSubDomains(true)
            .preload(false)
            .requestMatcher(AnyRequestMatcher.INSTANCE)
            .and()
            .contentSecurityPolicy(
                "default-src 'self'; frame-src 'self' data:; script-src 'self'" +
                    " 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline';" +
                    " img-src 'self' data:; font-src 'self' data:"
            )
            .and()
            .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            .and()
            .permissionsPolicy().policy(
                "geolocation 'none'; midi 'none'; sync-xhr 'none'; microphone 'none'; camera" +
                    " 'none'; magnetometer 'none'; gyroscope 'none'; speaker 'none'; fullscreen 'self'; payment 'none'"
            )
            .and()
            .frameOptions()
            .deny()
            .and()
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/authorize").authenticated()
                    .requestMatchers("/").permitAll() // ohjaus etusivulle
                    .requestMatchers("/kirjaudu").permitAll()
                    .requestMatchers("/api/").permitAll()
                    .requestMatchers("/api/haka-yliopistot").permitAll()
                    .requestMatchers("/api/julkinen/**").permitAll()
                    .requestMatchers("/api/auth-info").denyAll()
                    .requestMatchers("/api/login/impersonate")
                    .hasAnyAuthority(VASTUUHENKILO, KOULUTTAJA, OPINTOHALLINNON_VIRKAILIJA)
                    .requestMatchers(HttpMethod.GET, "/api/erikoistuva-laakari/**")
                    .hasAnyAuthority(
                        ERIKOISTUVA_LAAKARI,
                        ERIKOISTUVA_LAAKARI_IMPERSONATED,
                        ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
                    )
                    .requestMatchers(
                        "/api/erikoistuva-laakari/tyoskentelyjaksot/**",
                        "/api/erikoistuva-laakari/teoriakoulutukset/**"
                    )
                    .hasAnyAuthority(
                        ERIKOISTUVA_LAAKARI,
                        ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
                    )
                    .requestMatchers("/api/erikoistuva-laakari/**")
                    .hasAuthority(ERIKOISTUVA_LAAKARI)
                    .requestMatchers("/api/kouluttaja/**").hasAuthority(KOULUTTAJA)
                    .requestMatchers("/api/vastuuhenkilo/**").hasAuthority(VASTUUHENKILO)
                    .requestMatchers("/api/tekninen-paakayttaja/**")
                    .hasAuthority(TEKNINEN_PAAKAYTTAJA)
                    .requestMatchers("/api/virkailija/**").hasAuthority(OPINTOHALLINNON_VIRKAILIJA)
                    .requestMatchers(HttpMethod.PUT, "/api/kayttaja")
                    .hasAnyAuthority(
                        ERIKOISTUVA_LAAKARI,
                        KOULUTTAJA,
                        VASTUUHENKILO,
                        OPINTOHALLINNON_VIRKAILIJA,
                        TEKNINEN_PAAKAYTTAJA
                    )
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/info").denyAll()
                    .requestMatchers("/management/**").hasAuthority(ADMIN)
            }

        if (env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) || env.activeProfiles.contains(
                SPRING_PROFILE_PRODUCTION
            )
        ) {
            val relyingPartyRegistrationRepository =
                applicationContext.getBean(RelyingPartyRegistrationRepository::class.java)
            val relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver =
                DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)
            httpConfiguration.saml2Login().authenticationConverter(
                Saml2AuthenticationTokenConverter(
                    relyingPartyRegistrationResolver
                )
            ).authenticationManager(ProviderManager(authenticationProvider))
                .defaultSuccessUrl("/", true)
                .failureUrl("/kirjaudu").and()
                .addFilterBefore(ElsaUriFilter(applicationProperties), CsrfFilter::class.java)
                .saml2Logout { saml2 ->
                    saml2.logoutRequest { request ->
                        request.logoutRequestResolver(
                            logoutRequestResolver(relyingPartyRegistrationResolver)
                        )
                    }.logoutUrl("/api/logout").logoutResponse { response ->
                        response.logoutResponseResolver(
                            logoutResponseResolver(relyingPartyRegistrationResolver)
                        )
                    }
                }.logout().logoutSuccessUrl("/")
        }
        return http.build()
    }

    @Bean
    @Profile("dev", "prod")
    fun authenticationRequestResolver(
        registrations: RelyingPartyRegistrationRepository
    ): Saml2AuthenticationRequestResolver? {
        val registrationResolver: RelyingPartyRegistrationResolver =
            DefaultRelyingPartyRegistrationResolver(registrations)
        val authenticationRequestResolver =
            OpenSaml4AuthenticationRequestResolver(registrationResolver)
        authenticationRequestResolver.setRelayStateResolver {
            val attr = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            attr.request.getParameter("RelayState") ?: DEFAULT_RELAY_STATE
        }
        authenticationRequestResolver.setAuthnRequestCustomizer { context: AuthnRequestContext ->
            if (context.authnRequest.issuer.value != null && context.authnRequest.issuer.value!!.contains(
                    "haka"
                )
            ) {
                context.authnRequest.issuer.value = context.authnRequest.issuer.value!!.substring(
                    0,
                    context.authnRequest.issuer.value!!.indexOf("haka")
                ) + "haka"
            }
        }
        return authenticationRequestResolver
    }

    fun logoutRequestResolver(relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver): Saml2LogoutRequestResolver {
        val logoutRequestResolver = OpenSaml4LogoutRequestResolver(relyingPartyRegistrationResolver)
        logoutRequestResolver.setParametersConsumer { parameters ->
            val logoutRequest = parameters.logoutRequest
            val nameId = logoutRequest.nameID
            val principal = parameters.authentication.principal as Saml2AuthenticatedPrincipal
            nameId.value = principal.getFirstAttribute("nameID")
            nameId.format = principal.getFirstAttribute("nameIDFormat")
            nameId.nameQualifier = principal.getFirstAttribute("nameIDQualifier")
            nameId.spNameQualifier = principal.getFirstAttribute("nameIDSPQualifier")
        }
        return logoutRequestResolver
    }

    fun logoutResponseResolver(relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver): Saml2LogoutResponseResolver {
        return OpenSaml4LogoutResponseResolver(relyingPartyRegistrationResolver)
    }

    fun authenticationConverter(): Converter<OpenSaml4AuthenticationProvider.ResponseToken, AbstractAuthenticationToken> {
        return Converter { responseToken ->
            convertAuthentication(responseToken)
        }
    }

    fun convertAuthentication(responseToken: OpenSaml4AuthenticationProvider.ResponseToken): Saml2Authentication? {
        val token: Saml2Authentication =
            OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter()
                .convert(responseToken) as Saml2Authentication
        val principal = token.principal as Saml2AuthenticatedPrincipal
        val firstName = principal.attributes["urn:oid:2.5.4.42"]?.get(0) as String
        val lastName = principal.attributes["urn:oid:2.5.4.4"]?.get(0) as String

        // Suomi.fi
        val hetu = principal.attributes["urn:oid:1.2.246.21"]?.get(0) as String?

        // Haka
        val eppn = principal.attributes["urn:oid:1.3.6.1.4.1.5923.1.1.1.6"]?.get(0) as String?
        //val eduPersonAffiliation = principal.attributes["urn:oid:1.3.6.1.4.1.5923.1.1.1.1"]?.get(0)
        //val organization = principal.attributes["schacHomeOrganization"]?.get(0)

        val response = responseToken.response
        val assertion = CollectionUtils.firstElement(response.assertions)
        val nameID = assertion?.subject?.nameID
        principal.attributes["nameID"] = mutableListOf(nameID?.value) as List<*>?
        principal.attributes["nameIDFormat"] = mutableListOf(nameID?.format) as List<*>?
        principal.attributes["nameIDQualifier"] = mutableListOf(nameID?.nameQualifier) as List<*>?
        principal.attributes["nameIDSPQualifier"] =
            mutableListOf(nameID?.spNameQualifier) as List<*>?

        val decodedKey = Base64.getDecoder().decode(applicationProperties.getSecurity().encodedKey)
        val originalKey: SecretKey = SecretKeySpec(
            decodedKey, 0, decodedKey.size, applicationProperties.getSecurity().secretKeyAlgorithm
        )

        val cipher = Cipher.getInstance(applicationProperties.getSecurity().cipherAlgorithm)
        val attr = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        val verificationToken = attr.request.getParameter("RelayState")
        var tokenUser: User? = null

        // Kutsuttu käyttäjä
        if (verificationToken != null && verificationToken != DEFAULT_RELAY_STATE) {
            verificationTokenRepository.findById(verificationToken).ifPresent {
                tokenUser = userRepository.findByIdWithAuthorities(it.user?.id!!).get()
                userService.createOrUpdateUserWithToken(
                    tokenUser, it, cipher, originalKey, hetu, eppn, firstName, lastName
                )
            }
        }

        var existingUser = userService.findExistingUser(cipher, originalKey, hetu, eppn)

        if (shouldFetchOpintotietodata(existingUser, tokenUser, hetu)) {
            requireNotNull(hetu)

            if (existingUser == null) {
                fetchAndHandleOpintotietodataForFirstLogin(
                    cipher,
                    originalKey,
                    hetu,
                    firstName,
                    lastName
                )
                existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)

                // Lokaalissa ympäristössä luodaan uusi käyttäjä, jos sitä ei löydy.
                if (existingUser == null && env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT)) {
                    opintotietodataPersistenceService.createWithoutOpintotietodata(
                        cipher, originalKey, hetu, firstName, lastName
                    )
                    existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
                }

                existingUser?.let { user ->
                    fetchAndHandleOpintosuorituksetNonBlocking(user.id!!, hetu)
                }
            } else {
                fetchAndUpdateOpintotietodataIfChanged(existingUser.id!!, hetu, firstName, lastName)
                fetchAndHandleOpintosuorituksetNonBlocking(existingUser.id!!, hetu)

                if (hasErikoistuvaLaakariRole(existingUser)) {
                    opintooikeusService.checkOpintooikeusKaytossaValid(existingUser)
                }
            }
        }

        if (existingUser == null) {
            log.error(
                "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName (eppn $eppn). " +
                    "Käyttäjällä ei ole käyttöoikeutta."
            )
            throw Exception(LoginException.EI_KAYTTO_OIKEUTTA.name)
        }

        val kayttaja = kayttajaRepository.findOneByUserIdWithAuthorities(existingUser.id!!).orElseThrow {
            Exception(LoginException.EI_KAYTTO_OIKEUTTA.name)
        }

        if (kayttaja.tila == KayttajatilinTila.PASSIIVINEN) {
            log.error("Kirjautuminen epäonnistui käyttäjälle $firstName $lastName. Käyttäjän tili on passivoitu")
            throw Exception(LoginException.TILI_PASSIVOITU.name)
        }

        existingUser = kayttaja.user!!

        // Erikoistuvalla lääkärillä täytyy olla olemassaoleva opinto-oikeus
        if (hasErikoistuvaLaakariRole(existingUser) && !opintooikeusService.onOikeus(existingUser)) {
            if (hasKouluttajaRole(existingUser)) {
                existingUser.authorities = existingUser.authorities.filter { it.name != ERIKOISTUVA_LAAKARI }.toMutableSet()
                existingUser.activeAuthority = Authority(name = KOULUTTAJA)
                userRepository.save(existingUser)
            } else {
                log.error(
                    "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName. " + "Käyttäjällä ei ole voimassaolevaa " +
                        "opinto-oikeutta, opinto-oikeuden tila ei salli kirjautumista tai opinto-oikeuden erikoisala " +
                        "ei ole liittynyt Elsaan."
                )
                throw Exception(LoginException.EI_OPINTO_OIKEUTTA.name)
            }
        }

        if (hasVastuuhenkiloRole(existingUser) && kayttaja.tila == KayttajatilinTila.KUTSUTTU) {
            kayttaja.tila = KayttajatilinTila.AKTIIVINEN
            kayttajaRepository.save(kayttaja)
        }

        return Saml2Authentication(createPrincipal(kayttaja.user?.id, principal),
            token.saml2Response,
            kayttaja.user?.authorities?.map { SimpleGrantedAuthority(it.name) })
    }

    private fun shouldFetchOpintotietodata(
        existingUser: User?,
        tokenUser: User?,
        hetu: String?
    ): Boolean =
        (existingUser == null || hasErikoistuvaLaakariRole(existingUser)
            || hasKouluttajaRole(existingUser))
            && (tokenUser == null || hasErikoistuvaLaakariRole(tokenUser)) && hetu != null

    private fun hasErikoistuvaLaakariRole(user: User): Boolean =
        user.authorities.contains(Authority(name = ERIKOISTUVA_LAAKARI))

    private fun hasKouluttajaRole(user: User): Boolean =
        user.authorities.contains(Authority(name = KOULUTTAJA))

    private fun hasVastuuhenkiloRole(user: User): Boolean =
        user.authorities.contains(Authority(name = VASTUUHENKILO))

    private fun createPrincipal(
        name: String?, principal: Saml2AuthenticatedPrincipal
    ): DefaultSaml2AuthenticatedPrincipal {
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(name, principal.attributes)
        newPrincipal.relyingPartyRegistrationId = principal.relyingPartyRegistrationId
        return newPrincipal
    }

    private fun fetchAndHandleOpintotietodataForFirstLogin(
        cipher: Cipher, originalKey: SecretKey, hetu: String, firstName: String, lastName: String
    ) {
        runBlocking {
            supervisorScope {
                try {
                    val deferreds: List<Deferred<OpintotietodataDTO?>> =
                        opintotietodataFetchingServices.filter { it.shouldFetchOpintotietodata() }
                            .map { service ->
                                async(Dispatchers.IO) {
                                    service.fetchOpintotietodata(hetu)
                                }
                            }

                    deferreds.awaitAll().filterNotNull().let {
                        opintotietodataPersistenceService.create(
                            cipher,
                            originalKey,
                            hetu,
                            firstName,
                            lastName,
                            it
                        )
                    }
                } catch (ex: Exception) {
                    log.error("Virhe opintotietodatan haussa tai tallentamisessa: ${ex.message} ${ex.stackTrace}")
                }
            }
        }
    }

    private fun fetchAndUpdateOpintotietodataIfChanged(
        userId: String,
        hetu: String,
        firstName: String,
        lastName: String
    ) {
        runBlocking {
            supervisorScope {
                try {
                    val deferreds: List<Deferred<OpintotietodataDTO?>> =
                        opintotietodataFetchingServices.filter { it.shouldFetchOpintotietodata() }
                            .map { service ->
                                async(Dispatchers.IO) {
                                    service.fetchOpintotietodata(hetu)
                                }
                            }

                    deferreds.awaitAll().filterNotNull().let {
                        if (it.isNotEmpty()) {
                            opintotietodataPersistenceService.createOrUpdateIfChanged(
                                userId,
                                firstName,
                                lastName,
                                it
                            )
                        } else {
                            log.info("Kirjautuessa ladatut opintotiedot olivat tyhjät käyttäjälle: $userId")
                        }
                    }
                } catch (ex: Exception) {
                    log.error("Virhe opintotietodatan haussa tai päivittämisessä: ${ex.message} ${ex.stackTrace}")
                }
            }
        }
    }

    private fun fetchAndHandleOpintosuorituksetNonBlocking(userId: String, hetu: String) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            opintosuorituksetFetchingService.filter { it.shouldFetchOpintosuoritukset() }
                .map { service ->
                    scope.launch {
                        service.fetchOpintosuoritukset(hetu)?.let {
                            opintosuorituksetPersistenceService.createOrUpdateIfChanged(userId, it)
                        }
                    }
                }
        } catch (ex: Exception) {
            log.error("Virhe opintosuoritusten haussa tai tallentamisessa: ${ex.message} ${ex.stackTrace}")
        }
    }

    private fun assertionContainsInResponseTo(assertion: Assertion): Boolean {
        if (assertion.subject == null) {
            return false
        }
        for (confirmation in assertion.subject.subjectConfirmations) {
            val confirmationData = confirmation.subjectConfirmationData ?: continue
            if (StringUtils.hasText(confirmationData.inResponseTo)) {
                return true
            }
        }
        return false
    }
}
