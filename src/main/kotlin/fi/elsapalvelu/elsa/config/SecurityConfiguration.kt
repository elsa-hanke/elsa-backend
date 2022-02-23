package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.enumeration.ApplicationSettingTyyppi
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.OpintotietoDataDTO
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.opensaml.saml.common.assertion.ValidationContext
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.*
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutResponseResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutResponseResolver
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import org.springframework.util.CollectionUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.filter.CorsFilter
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_DEVELOPMENT
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_PRODUCTION
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Import(SecurityProblemSupport::class)
class SecurityConfiguration(
    private val corsFilter: CorsFilter,
    private val applicationProperties: ApplicationProperties,
    private val problemSupport: SecurityProblemSupport,
    private val userService: UserService,
    private val opintooikeusService: OpintooikeusService,
    private val env: Environment,
    private val applicationSettingService: ApplicationSettingService,
    private val opintotietoDataFetchingServices: List<OpintotietoDataFetchingService>,
    private val opintotietoDataPersistenceService: OpintotietoDataPersistenceService
) : WebSecurityConfigurerAdapter() {

    private val log = LoggerFactory.getLogger(SecurityConfiguration::class.java)

    override fun configure(web: WebSecurity?) {
        web!!.ignoring().antMatchers(HttpMethod.OPTIONS, "/**").antMatchers("/h2-console/**").antMatchers("/test/**")
    }

    @Throws(Exception::class)
    public override fun configure(http: HttpSecurity) {
        val withHttpOnlyFalse = CookieCsrfTokenRepository.withHttpOnlyFalse()
        val authenticationProvider = OpenSaml4AuthenticationProvider()
        authenticationProvider.setAssertionValidator(OpenSaml4AuthenticationProvider.createDefaultAssertionValidator {
            val relyingPartyRegistration: RelyingPartyRegistration = it.token.relyingPartyRegistration
            val audience = relyingPartyRegistration.entityId
            val recipient = relyingPartyRegistration.assertionConsumerServiceLocation
            val assertingPartyEntityId = relyingPartyRegistration.assertingPartyDetails.entityId
            val params: MutableMap<String, Any> = HashMap()
            params[SAML2AssertionValidationParameters.COND_VALID_AUDIENCES] = setOf(
                if (audience.contains("haka")) audience.substring(
                    0, audience.indexOf("haka")
                ) + "haka"; else audience
            )
            params[SAML2AssertionValidationParameters.SC_VALID_RECIPIENTS] = setOf(recipient)
            params[SAML2AssertionValidationParameters.VALID_ISSUERS] = setOf(assertingPartyEntityId)
            ValidationContext(params)
        })
        authenticationProvider.setResponseAuthenticationConverter(authenticationConverter())
        withHttpOnlyFalse.setCookieDomain(applicationProperties.getCsrf().cookie.domain)
        val httpConfiguration =
            http.csrf().csrfTokenRepository(withHttpOnlyFalse).ignoringAntMatchers("/api/logout").and()
                .addFilterBefore(corsFilter, CsrfFilter::class.java).exceptionHandling()
                .authenticationEntryPoint(problemSupport).accessDeniedHandler(problemSupport).and().headers()
                .httpStrictTransportSecurity()
                // 12 months
                .maxAgeInSeconds(31536000).includeSubDomains(true).preload(false)
                .requestMatcher(AnyRequestMatcher.INSTANCE).and().contentSecurityPolicy(
                    "default-src 'self'; frame-src 'self' data:; script-src 'self'" + " 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline';" + " img-src 'self' data:; font-src 'self' data:"
                ).and().referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN).and()
                .permissionsPolicy().policy(
                    "geolocation 'none'; midi 'none'; sync-xhr 'none'; microphone 'none'; camera" + " 'none'; magnetometer 'none'; gyroscope 'none'; speaker 'none'; fullscreen 'self'; payment 'none'"
                ).and().frameOptions().deny().and().authorizeRequests().antMatchers("/authorize").authenticated()
                .antMatchers("/api/").permitAll() // ohjaus etusivulle
                .antMatchers("/api/haka-yliopistot").permitAll().antMatchers("/api/auth-info").denyAll()
                .antMatchers("/api/erikoistuva-laakari/kayttooikeushakemus").authenticated()
                .antMatchers("/api/erikoistuva-laakari/**").hasAuthority(ERIKOISTUVA_LAAKARI)
                .antMatchers("/api/kouluttaja/**").hasAuthority(KOULUTTAJA).antMatchers("/api/vastuuhenkilo/**")
                .hasAuthority(VASTUUHENKILO).antMatchers("/api/tekninen-paakayttaja/**")
                .hasAuthority(TEKNINEN_PAAKAYTTAJA).antMatchers("/api/**").authenticated()
                .antMatchers("/management/health").permitAll().antMatchers("/management/info").denyAll()
                .antMatchers("/management/prometheus").denyAll().antMatchers("/management/**").hasAuthority(ADMIN)

        if (env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) || env.activeProfiles.contains(
                SPRING_PROFILE_PRODUCTION
            )
        ) {
            val relyingPartyRegistrationRepository =
                applicationContext.getBean(RelyingPartyRegistrationRepository::class.java)
            val relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver =
                DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)
            httpConfiguration.and().saml2Login().authenticationConverter(
                Saml2AuthenticationTokenConverter(
                    relyingPartyRegistrationResolver
                )
            ).authenticationManager(ProviderManager(authenticationProvider)).defaultSuccessUrl("/", true)
                .failureUrl("/kirjaudu").and()
                .addFilterBefore(ElsaUriFilter(applicationProperties), CsrfFilter::class.java).saml2Logout { saml2 ->
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
    }

    @Bean
    fun authenticationRequestFactory(
        authnRequestConverter: AuthnRequestConverter?
    ): Saml2AuthenticationRequestFactory? {
        val authenticationRequestFactory = OpenSaml4AuthenticationRequestFactory()
        authenticationRequestFactory.setAuthenticationRequestContextConverter(authnRequestConverter)
        return authenticationRequestFactory
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
        val token: Saml2Authentication = OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter()
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
        principal.attributes["nameIDSPQualifier"] = mutableListOf(nameID?.spNameQualifier) as List<*>?

        val decodedKey = Base64.getDecoder().decode(applicationProperties.getSecurity().encodedKey)
        val originalKey: SecretKey = SecretKeySpec(
            decodedKey, 0, decodedKey.size, applicationProperties.getSecurity().secretKeyAlgorithm
        )

        val cipher = Cipher.getInstance(applicationProperties.getSecurity().cipherAlgorithm)
        val attr = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        val verificationToken = attr.request.getParameter("RelayState")

        // Kutsuttu käyttäjä
        if (verificationToken != null) {
            userService.createOrUpdateUserWithToken(
                verificationToken, cipher, originalKey, hetu, eppn, firstName, lastName
            )
        }

        // Käyttäjä täytyy löytyä järjestelmästä
        var existingUser = userService.findExistingUser(cipher, originalKey, hetu, eppn)

        hetu?.let {
            val shouldQueryOpintotietoData =
                applicationSettingService.getBooleanSettingValue(ApplicationSettingTyyppi.QUERY_OPINTOTIETO_DATA) ?: true
            val isDevelopment = env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT)
            val isProduction = env.activeProfiles.contains(SPRING_PROFILE_PRODUCTION)

            if (existingUser == null) {
                // Lokaalissa ympäristössä luodaan uusi käyttäjä, jos sitä ei löydy ja kannassa oleva asetus
                // APP_SETTING_QUERY_OPINTOTIETO_DATA = false
                if (isDevelopment && !shouldQueryOpintotietoData) {
                    opintotietoDataPersistenceService.createWithoutOpintotietoData(
                        cipher, originalKey, it, firstName, lastName
                    )
                } else {
                    fetchAndHandleOpintotietoDataForFirstLogin(cipher, originalKey, hetu, firstName, lastName)
                }
            }
            // Tuotannossa haetaan aina data opintotietojärjestelmistä, muutoin tehdään haku vain jos kannassa oleva
            // asetus APP_SETTING_QUERY_OPINTOTIETO_DATA = true
            else if (isProduction || shouldQueryOpintotietoData) {
                fetchAndUpdateOpintotietoDataIfChanged(existingUser?.id!!, hetu, firstName, lastName)
            }

            existingUser = userService.findExistingUser(cipher, originalKey, hetu, null)
        }

        if (existingUser == null) {
            log.error(
                "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName (eppn $eppn). " + "Käyttäjällä ei ole käyttöoikeutta."
            )
            throw Exception(LoginException.EI_KAYTTO_OIKEUTTA.name)
        }

        // Erikoistuvalla lääkärillä täytyy olla olemassaoleva opinto-oikeus
        if (!opintooikeusService.onOikeus(existingUser!!)) {
            log.error(
                "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName. " + "Käyttäjällä ei ole aktiivista opinto-oikeutta."
            )
            throw Exception(LoginException.EI_OPINTO_OIKEUTTA.name)
        }

        return Saml2Authentication(createPrincipal(existingUser!!.id, principal),
            token.saml2Response,
            existingUser!!.authorities.map { SimpleGrantedAuthority(it.name) })
    }

    private fun createPrincipal(
        name: String?, principal: Saml2AuthenticatedPrincipal
    ): DefaultSaml2AuthenticatedPrincipal {
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(name, principal.attributes)
        newPrincipal.relyingPartyRegistrationId = principal.relyingPartyRegistrationId
        return newPrincipal
    }

    private fun fetchAndHandleOpintotietoDataForFirstLogin(
        cipher: Cipher, originalKey: SecretKey, hetu: String, firstName: String, lastName: String
    ) {
        runBlocking {
            // TODO: Lisää käsittely tilanteelle, jossa opintotietodataa ei saatu jostakin opintotietojärjestelmästä
            val deferreds: List<Deferred<OpintotietoDataDTO?>> =
                opintotietoDataFetchingServices.filter { it.shouldFetchOpintotietoData() }.map { service ->
                    async {
                        service.fetchOpintotietoData(hetu)
                    }
                }

            deferreds.awaitAll().map {
                it?.let {
                    opintotietoDataPersistenceService.create(cipher, originalKey, hetu, firstName, lastName, it)
                }
            }
        }
    }

    private fun fetchAndUpdateOpintotietoDataIfChanged(
        userId: String,
        hetu: String,
        firstName: String,
        lastName: String
    ) {
        runBlocking {
            val deferreds: List<Deferred<OpintotietoDataDTO?>> =
                opintotietoDataFetchingServices.filter { it.shouldFetchOpintotietoData() }.map { service ->
                    async {
                        service.fetchOpintotietoData(hetu)
                    }
                }

            deferreds.awaitAll().map {
                it?.let {
                    opintotietoDataPersistenceService.createOrUpdateIfChanged(userId, firstName, lastName, it)
                }
            }
        }
    }

}
