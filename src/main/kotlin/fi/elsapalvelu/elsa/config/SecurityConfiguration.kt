package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.audit.AuditLoggingWrapper
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen.Kirjautumispalvelu
import fi.elsapalvelu.elsa.service.kayttaja.kirjautuminen.KIRJAUTUMISEN_OLETUS_RELAY_STATE
import jakarta.servlet.http.HttpServletResponse
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters
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
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml5AuthenticationRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml5AuthenticationRequestResolver.AuthnRequestContext
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml5LogoutRequestResolver
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml5LogoutResponseResolver
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
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.filter.CorsFilter
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_DEVELOPMENT
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_PRODUCTION

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfiguration(
    private val corsFilter: CorsFilter,
    private val applicationProperties: ApplicationProperties,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val env: Environment,
    private val applicationContext: ApplicationContext,
    private val kirjautumispalvelu: Kirjautumispalvelu
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        configureCsrf(http)
        configureFilters(http)
        configureExceptionHandling(http)
        configureHeaders(http)
        configureAuthorization(http)

        if (env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) || env.activeProfiles.contains(SPRING_PROFILE_PRODUCTION)) {
            configureSaml(http)
        }
        return http.build()
    }

    private fun configureCsrf(http: HttpSecurity) {
        val withHttpOnlyFalse = CookieCsrfTokenRepository.withHttpOnlyFalse()
        withHttpOnlyFalse.setCookieCustomizer { c -> c.domain(applicationProperties.getCsrf().cookie.domain) }
        val requestHandler = CsrfTokenRequestAttributeHandler()
        requestHandler.setCsrfRequestAttributeName(null)
        http.csrf { csrf ->
            csrf
                .csrfTokenRequestHandler(requestHandler)
                .csrfTokenRepository(withHttpOnlyFalse)
                .ignoringRequestMatchers("/api/logout")
        }
    }

    private fun configureFilters(http: HttpSecurity) {
        http
            .addFilterBefore(corsFilter, CsrfFilter::class.java)
            .addFilterAfter(
                applicationProperties.getCsrf().cookie.domain?.let {
                    StaleXsrfCookieClearingFilter(it)
                } ?: StaleXsrfCookieClearingFilter(""),
                CsrfFilter::class.java
            )
            .addFilterAfter(MdcUserIdFilter(), CorsFilter::class.java)
            .addFilterAfter(
                ElsaSwitchUserFilter(
                    opintooikeusRepository,
                    kayttajaRepository,
                    userRepository,
                    kouluttajavaltuutusRepository
                ),
                AuthorizationFilter::class.java
            )
    }

    private fun configureExceptionHandling(http: HttpSecurity) {
        http.exceptionHandling { ex ->
            ex.accessDeniedHandler { request, response, _ ->
                AuditLoggingWrapper.warn(
                    "Access denied for " +
                        "user: ${request.userPrincipal?.name}, " +
                        "method: ${request.method}, " +
                        "path: ${request.requestURI}}, " +
                        "ip: ${request.getHeader("X-Forwarded-For")}"
                )
                response.sendError(HttpServletResponse.SC_FORBIDDEN)
            }
                .authenticationEntryPoint { _, response, _ ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                }
        }
    }

    private fun configureHeaders(http: HttpSecurity) {
        http.headers { h ->
            h.httpStrictTransportSecurity { sec ->
                // 12 months
                sec.maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(false)
                    .requestMatcher(AnyRequestMatcher.INSTANCE)
            }
                .contentSecurityPolicy { p ->
                    p.policyDirectives("default-src 'self'; frame-src 'self' data:; script-src 'self'" +
                        " 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline';" +
                        " img-src 'self' data:; font-src 'self' data:")
                }
                .referrerPolicy { p ->
                    p.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                }
                .permissionsPolicyHeader { p ->
                    p.policy("geolocation 'none'; midi 'none'; sync-xhr 'none'; microphone 'none'; camera" +
                        " 'none'; magnetometer 'none'; gyroscope 'none'; speaker 'none'; fullscreen 'self'; payment 'none'")
                }
            h.frameOptions { o ->
                o.deny()
            }
        }
    }

    private fun configureAuthorization(http: HttpSecurity) {
        http.authorizeHttpRequests { authorize ->
            authorize
                .requestMatchers("/authorize").authenticated()
                .requestMatchers("/").permitAll() // ohjaus etusivulle
                .requestMatchers("/kirjaudu").permitAll()
                .requestMatchers("/api/").permitAll()
                .requestMatchers("/api/ping").permitAll()
                .requestMatchers("/api/haka-yliopistot").permitAll()
                .requestMatchers("/api/julkinen/**").permitAll()
                .requestMatchers("/api/auth-info").denyAll()
                .requestMatchers("/api/login/impersonate")
                .hasAnyAuthority(VASTUUHENKILO, KOULUTTAJA, OPINTOHALLINNON_VIRKAILIJA)
                .requestMatchers(HttpMethod.GET, "/api/erikoistuva-laakari")
                .hasAnyAuthority(ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA, ERIKOISTUVA_LAAKARI_IMPERSONATED, ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
                .requestMatchers(HttpMethod.PUT, "/api/erikoistuva-laakari")
                .hasAnyAuthority(ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA)
                .requestMatchers(HttpMethod.POST, "/api/erikoistuva-laakari/muokkausoikeudet")
                .hasAnyAuthority(ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA)
                .requestMatchers(HttpMethod.GET, "/api/erikoistuva-laakari/**")
                .hasAnyAuthority(ERIKOISTUVA_LAAKARI, ERIKOISTUVA_LAAKARI_IMPERSONATED, ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
                .requestMatchers(
                    "/api/erikoistuva-laakari/tyoskentelyjaksot/**",
                    "/api/erikoistuva-laakari/teoriakoulutukset/**"
                )
                .hasAnyAuthority(ERIKOISTUVA_LAAKARI, ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
                .requestMatchers("/api/erikoistuva-laakari/**").hasAuthority(ERIKOISTUVA_LAAKARI)
                .requestMatchers(HttpMethod.GET, "/api/yek-koulutettava/**")
                .hasAnyAuthority(YEK_KOULUTETTAVA, ERIKOISTUVA_LAAKARI_IMPERSONATED, ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
                .requestMatchers(
                    "/api/yek-koulutettava/tyoskentelyjaksot/**",
                    "/api/yek-koulutettava/laillistamispaiva"
                )
                .hasAnyAuthority(YEK_KOULUTETTAVA, ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)
                .requestMatchers("/api/yek-koulutettava/**").hasAuthority(YEK_KOULUTETTAVA)
                .requestMatchers("/api/kouluttaja/**").hasAuthority(KOULUTTAJA)
                .requestMatchers("/api/vastuuhenkilo/**").hasAuthority(VASTUUHENKILO)
                .requestMatchers("/api/tekninen-paakayttaja/**")
                .hasAuthority(TEKNINEN_PAAKAYTTAJA)
                .requestMatchers("/api/virkailija/**").hasAuthority(OPINTOHALLINNON_VIRKAILIJA)
                .requestMatchers(HttpMethod.PUT, "/api/kayttaja")
                .hasAnyAuthority(ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA, KOULUTTAJA, VASTUUHENKILO, OPINTOHALLINNON_VIRKAILIJA, TEKNINEN_PAAKAYTTAJA)
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/management/health").permitAll()
                .requestMatchers("/management/info").denyAll()
                .requestMatchers("/management/**").hasAuthority(ADMIN)
        }
    }

    private fun configureSaml(http: HttpSecurity) {
        val authenticationProvider = OpenSaml5AuthenticationProvider()
        authenticationProvider.setAssertionValidator(createAssertionValidator())
        authenticationProvider.setResponseAuthenticationConverter(authenticationConverter())
        val relyingPartyRegistrationRepository =
            applicationContext.getBean(RelyingPartyRegistrationRepository::class.java)
        val relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver =
            DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)
        http.saml2Login { l ->
            l.authenticationConverter(Saml2AuthenticationTokenConverter(relyingPartyRegistrationResolver))
                .authenticationManager(ProviderManager(authenticationProvider))
                .defaultSuccessUrl("/", true)
                .failureUrl("/kirjaudu")
        }
        http
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
            }.logout { l -> l.logoutSuccessUrl("/") }
    }

    @Bean
    @Profile("dev", "prod")
    fun authenticationRequestResolver(
        registrations: RelyingPartyRegistrationRepository
    ): Saml2AuthenticationRequestResolver? {
        val registrationResolver: RelyingPartyRegistrationResolver =
            DefaultRelyingPartyRegistrationResolver(registrations)
        val authenticationRequestResolver =
            OpenSaml5AuthenticationRequestResolver(registrationResolver)
        authenticationRequestResolver.setRelayStateResolver {
            val attr = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            attr.request.getParameter("RelayState") ?: KIRJAUTUMISEN_OLETUS_RELAY_STATE
        }
        authenticationRequestResolver.setAuthnRequestCustomizer { context: AuthnRequestContext ->
            val issuer = context.authnRequest.issuer.required()
            val value = issuer.value
            if (value != null && value.contains("haka")) {
                issuer.value = value.substring(0, value.indexOf("haka")) + "haka"
            }
        }
        return authenticationRequestResolver
    }

    // Retain the legacy SAML principal until login, logout and impersonation migrate together.
    @Suppress("DEPRECATION", "Deprecation")
    fun logoutRequestResolver(relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver): Saml2LogoutRequestResolver {
        val logoutRequestResolver = OpenSaml5LogoutRequestResolver(relyingPartyRegistrationResolver)
        logoutRequestResolver.setParametersConsumer { parameters ->
            val logoutRequest = parameters.logoutRequest
            val nameId = logoutRequest.nameID.required()
            val principal = parameters.authentication.principal as Saml2AuthenticatedPrincipal
            nameId.value = principal.getFirstAttribute("nameID")
            nameId.format = principal.getFirstAttribute("nameIDFormat")
            nameId.nameQualifier = principal.getFirstAttribute("nameIDQualifier")
            nameId.spNameQualifier = principal.getFirstAttribute("nameIDSPQualifier")
        }
        return logoutRequestResolver
    }

    fun logoutResponseResolver(relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver): Saml2LogoutResponseResolver {
        return OpenSaml5LogoutResponseResolver(relyingPartyRegistrationResolver)
    }

    fun authenticationConverter(): Converter<OpenSaml5AuthenticationProvider.ResponseToken, AbstractAuthenticationToken> {
        return Converter { responseToken ->
            convertAuthentication(responseToken)
        }
    }

    // Legacy SAML principal compatibility; migrate with login, logout and impersonation.
    @Suppress("DEPRECATION", "Deprecation")
    fun convertAuthentication(responseToken: OpenSaml5AuthenticationProvider.ResponseToken): Saml2Authentication {
        val token: Saml2Authentication =
            OpenSaml5AuthenticationProvider.ResponseAuthenticationConverter()
                .convert(responseToken)
        val registrationId = responseToken.token.relyingPartyRegistration.registrationId
        val principal = createPrincipal(token.name, token.principal as Saml2AuthenticatedPrincipal, registrationId)
        val firstName = principal.attributes["urn:oid:2.5.4.42"]?.get(0) as String
        val lastName = principal.attributes["urn:oid:2.5.4.4"]?.get(0) as String

        // Suomi.fi
        val hetu = principal.attributes["urn:oid:1.2.246.21"]?.get(0) as String?
        // Haka
        val eppn = principal.attributes["urn:oid:1.3.6.1.4.1.5923.1.1.1.6"]?.get(0) as String?

        addLogoutNameIdAttributes(principal, responseToken)

        val kirjautumistulos = kirjautumispalvelu.kirjaudu(hetu, eppn, firstName, lastName)
        return Saml2Authentication(createPrincipal(kirjautumistulos.kayttaja.id, principal, registrationId), token.saml2Response,
            kirjautumistulos.roolit.map { SimpleGrantedAuthority(it.name.required()) })
    }

    @Suppress("DEPRECATION", "Deprecation")
    private fun addLogoutNameIdAttributes(
        principal: DefaultSaml2AuthenticatedPrincipal,
        responseToken: OpenSaml5AuthenticationProvider.ResponseToken
    ) {
        val assertion = CollectionUtils.firstElement(responseToken.response.assertions)
        val nameID = assertion?.subject?.nameID
        principal.attributes["nameID"] = mutableListOf(nameID?.value) as List<*>?
        principal.attributes["nameIDFormat"] = mutableListOf(nameID?.format) as List<*>?
        principal.attributes["nameIDQualifier"] = mutableListOf(nameID?.nameQualifier) as List<*>?
        principal.attributes["nameIDSPQualifier"] = mutableListOf(nameID?.spNameQualifier) as List<*>?
    }

    // Retain the legacy SAML principal until login, logout and impersonation migrate together.
    @Suppress("DEPRECATION", "Deprecation")
    internal fun createPrincipal(
        name: String?, principal: Saml2AuthenticatedPrincipal, registrationId: String
    ): DefaultSaml2AuthenticatedPrincipal {
        // Security 7 stores the registration on the authentication token, not its principal.
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(name, principal.attributes.toMutableMap(), principal.sessionIndexes)
        newPrincipal.relyingPartyRegistrationId = registrationId
        return newPrincipal
    }

    internal fun createAssertionValidator(): Converter<OpenSaml5AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> {
        return Converter { assertionToken ->
            val audience = assertionToken.token.relyingPartyRegistration.entityId
            val normalizedAudience = if (audience.contains("haka")) audience.substringBefore("haka") + "haka" else audience
            val validator = OpenSaml5AuthenticationProvider.AssertionValidator.builder()
                .validationContextParameters { parameters ->
                    parameters[SAML2AssertionValidationParameters.COND_VALID_AUDIENCES] = setOf(normalizedAudience)
                }
                .build()
            validator.convert(assertionToken)
        }
    }
}
