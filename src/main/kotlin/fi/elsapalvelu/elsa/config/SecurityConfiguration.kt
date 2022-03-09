package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.*
import org.apache.commons.text.similarity.LevenshteinDistance
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
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor
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
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Import(SecurityProblemSupport::class)
class SecurityConfiguration(
    private val corsFilter: CorsFilter,
    private val applicationProperties: ApplicationProperties,
    private val problemSupport: SecurityProblemSupport,
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val env: Environment
) : WebSecurityConfigurerAdapter() {

    private val log = LoggerFactory.getLogger(SecurityConfiguration::class.java)

    override fun configure(web: WebSecurity?) {
        web!!.ignoring()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .antMatchers("/h2-console/**")
            .antMatchers("/test/**")
    }

    @Throws(Exception::class)
    public override fun configure(http: HttpSecurity) {
        val withHttpOnlyFalse = CookieCsrfTokenRepository.withHttpOnlyFalse()
        val authenticationProvider = OpenSaml4AuthenticationProvider()
        authenticationProvider.setAssertionValidator(
            OpenSaml4AuthenticationProvider
                .createDefaultAssertionValidator {
                    val relyingPartyRegistration: RelyingPartyRegistration =
                        it.token.relyingPartyRegistration
                    val audience = relyingPartyRegistration.entityId
                    val recipient = relyingPartyRegistration.assertionConsumerServiceLocation
                    val assertingPartyEntityId =
                        relyingPartyRegistration.assertingPartyDetails.entityId
                    val params: MutableMap<String, Any> = HashMap()
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
        val httpConfiguration = http
            .csrf()
            .csrfTokenRepository(withHttpOnlyFalse)
            .ignoringAntMatchers("/api/logout")
            .and()
            .addFilterBefore(corsFilter, CsrfFilter::class.java)
            .exceptionHandling()
            .authenticationEntryPoint(problemSupport)
            .accessDeniedHandler(problemSupport)
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
            .authorizeRequests()
            .antMatchers("/authorize").authenticated()
            .antMatchers("/api/").permitAll() // ohjaus etusivulle
            .antMatchers("/api/haka-yliopistot").permitAll()
            .antMatchers("/api/julkinen/**").permitAll()
            .antMatchers("/api/auth-info").denyAll()
            .antMatchers("/api/login/impersonate").hasAnyAuthority(VASTUUHENKILO, KOULUTTAJA)
            .antMatchers("/api/erikoistuva-laakari/kayttooikeushakemus").authenticated()
            .antMatchers(HttpMethod.GET, "/api/erikoistuva-laakari/**")
            .hasAnyAuthority(ERIKOISTUVA_LAAKARI, ERIKOISTUVA_LAAKARI_IMPERSONATED)
            .antMatchers("/api/erikoistuva-laakari/**").hasAuthority(ERIKOISTUVA_LAAKARI)
            .antMatchers("/api/kouluttaja/**").hasAuthority(KOULUTTAJA)
            .antMatchers("/api/vastuuhenkilo/**").hasAuthority(VASTUUHENKILO)
            .antMatchers("/api/tekninen-paakayttaja/**").hasAuthority(TEKNINEN_PAAKAYTTAJA)
            .antMatchers("/api/**").authenticated()
            .antMatchers("/management/health").permitAll()
            .antMatchers("/management/info").denyAll()
            .antMatchers("/management/prometheus").denyAll()
            .antMatchers("/management/**").hasAuthority(ADMIN)

        if (env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) || env.activeProfiles.contains(
                SPRING_PROFILE_PRODUCTION
            )
        ) {
            val relyingPartyRegistrationRepository =
                applicationContext.getBean(RelyingPartyRegistrationRepository::class.java)
            val relyingPartyRegistrationResolver: RelyingPartyRegistrationResolver =
                DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)
            httpConfiguration.and().saml2Login()
                .authenticationConverter(
                    Saml2AuthenticationTokenConverter(
                        relyingPartyRegistrationResolver
                    )
                )
                .authenticationManager(ProviderManager(authenticationProvider))
                .defaultSuccessUrl("/", true)
                .failureUrl("/kirjaudu")
                .and()
                .addFilterBefore(ElsaUriFilter(applicationProperties), CsrfFilter::class.java)
                .saml2Logout { saml2 ->
                    saml2.logoutRequest { request ->
                        request.logoutRequestResolver(
                            logoutRequestResolver(relyingPartyRegistrationResolver)
                        )
                    }.logoutUrl("/api/logout")
                        .logoutResponse { response ->
                            response.logoutResponseResolver(
                                logoutResponseResolver(relyingPartyRegistrationResolver)
                            )
                        }
                }
                .logout().logoutSuccessUrl("/")
                .and()
                .addFilterAfter(
                    ElsaSwitchUserFilter(
                        erikoistuvaLaakariRepository,
                        kayttajaRepository,
                        userRepository,
                        kouluttajavaltuutusRepository
                    ),
                    FilterSecurityInterceptor::class.java
                )
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
        val token: Saml2Authentication = OpenSaml4AuthenticationProvider
            .createDefaultResponseAuthenticationConverter()
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

        val decodedKey =
            Base64.getDecoder().decode(applicationProperties.getSecurity().encodedKey)
        val originalKey: SecretKey = SecretKeySpec(
            decodedKey,
            0,
            decodedKey.size,
            applicationProperties.getSecurity().secretKeyAlgorithm
        )

        val cipher = Cipher.getInstance(applicationProperties.getSecurity().cipherAlgorithm)

        val attr = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        val verificationToken = attr.request.getParameter("RelayState")

        // Kutsuttu käyttäjä
        if (verificationToken != null) {
            verificationTokenRepository.findById(verificationToken)
                .ifPresent {
                    val tokenUser = userRepository.findByIdWithAuthorities(it.user?.id!!).get()

                    // Kutsutun käyttäjä etu- ja sukunimi tulee olla tarpeeksi lähellä
                    // kutsussa syötettyjä tietoja
                    val distance = LevenshteinDistance()
                    val firstNameDistFirst = distance.apply(tokenUser.firstName, firstName)
                    val firstNameDistLast = distance.apply(tokenUser.firstName, lastName)
                    val lastNameDistFirst = distance.apply(tokenUser.lastName, firstName)
                    val lastNameDistLast = distance.apply(tokenUser.lastName, lastName)
                    val treshhold = 2
                    if ((firstNameDistFirst > treshhold && firstNameDistLast > treshhold)
                        || (lastNameDistFirst > treshhold && lastNameDistLast > treshhold)
                    ) {
                        log.error(
                            "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName (eppn $eppn). " +
                                "Kutsussa annettu nimi ${tokenUser.firstName} ${tokenUser.lastName} ei ole" +
                                "tarpeeksi lähellä käyttäjän nimeä."
                        )
                        throw Exception(LoginException.VIRHEELLINEN_NIMI.name)
                    }

                    val existingUser = findExistingUser(cipher, originalKey, hetu, eppn)

                    // Yhdistä käyttäjä jos löytyy
                    if (existingUser != null) {
                        val existingKayttaja = kayttajaRepository.findOneByUserId(existingUser.id!!)
                        val tokenKayttaja = kayttajaRepository.findOneByUserId(tokenUser.id!!)

                        updateKouluttajaReferences(
                            tokenKayttaja.get().id!!,
                            existingKayttaja.get().id!!
                        )

                        existingUser.email = tokenUser.email
                        existingUser.authorities.clear()
                        existingUser.authorities.addAll(tokenUser.authorities)

                        kayttajaRepository.delete(tokenKayttaja.get())
                        verificationTokenRepository.delete(it)
                        userRepository.delete(tokenUser)
                        userRepository.save(existingUser)
                    } else {
                        cipher.init(Cipher.ENCRYPT_MODE, originalKey)
                        val params = cipher.parameters
                        val iv = params.getParameterSpec(IvParameterSpec::class.java).iv
                        val ciphertext = if (hetu != null)
                            cipher.doFinal(
                                hetu.toString().toByteArray(StandardCharsets.UTF_8)
                            ) else null

                        tokenUser.hetu = ciphertext
                        tokenUser.eppn = eppn
                        tokenUser.initVector = iv
                        tokenUser.firstName = firstName
                        tokenUser.lastName = lastName

                        userRepository.save(tokenUser)
                        verificationTokenRepository.delete(it)
                    }
                }
        }

        // Käyttäjä täytyy löytyä järjestelmästä
        var existingUser =
            findExistingUser(cipher, originalKey, hetu, eppn)

        // Lokaalissa ympäristössä luodaan uusi käyttäjä, jos sitä ei löydy
        if (existingUser == null) {
            if (env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT)) {
                cipher.init(Cipher.ENCRYPT_MODE, originalKey)
                val params = cipher.parameters
                val iv = params.getParameterSpec(IvParameterSpec::class.java).iv
                val ciphertext = cipher.doFinal(hetu.toString().toByteArray(StandardCharsets.UTF_8))
                userRepository.save(
                    User(
                        login = UUID.randomUUID().toString(),
                        firstName = firstName,
                        lastName = lastName,
                        activated = true,
                        hetu = ciphertext,
                        initVector = iv
                    )
                )
            }

            existingUser = findExistingUser(cipher, originalKey, hetu, eppn)
        }

        if (existingUser == null) {
            log.error(
                "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName (eppn $eppn). " +
                    "Käyttäjällä ei ole käyttöoikeutta."
            )
            throw Exception(LoginException.EI_KAYTTO_OIKEUTTA.name)
        }

        // Erikoistuvalla lääkärillä täytyy olla olemassaoleva opinto-oikeus
        if (!onOikeus(existingUser)) {
            log.error(
                "Kirjautuminen epäonnistui käyttäjälle $firstName $lastName. " +
                    "Käyttäjällä ei ole aktiivista opinto-oikeutta."
            )
            throw Exception(LoginException.EI_OPINTO_OIKEUTTA.name)
        }

        return Saml2Authentication(
            createPrincipal(existingUser.id, principal),
            token.saml2Response,
            existingUser.authorities.map { SimpleGrantedAuthority(it.name) }
        )
    }

    private fun onOikeus(user: User): Boolean {
        if (user.authorities.contains(Authority(name = ERIKOISTUVA_LAAKARI))) {
            return opintooikeusRepository.existsByErikoistuvaLaakariKayttajaUserId(
                user.id!!,
                LocalDate.now()
            )
        }
        return true
    }

    private fun createPrincipal(
        name: String?,
        principal: Saml2AuthenticatedPrincipal
    ): DefaultSaml2AuthenticatedPrincipal {
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(name, principal.attributes)
        newPrincipal.relyingPartyRegistrationId = principal.relyingPartyRegistrationId
        return newPrincipal
    }

    private fun findExistingUser(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        eppn: String?
    ): User? {
        if (hetu != null) {
            userRepository.findAllWithAuthorities().filter { u -> u.hetu != null }.forEach { u ->
                cipher.init(Cipher.DECRYPT_MODE, originalKey, IvParameterSpec(u.initVector))
                val userHetu = String(cipher.doFinal(u.hetu), StandardCharsets.UTF_8)
                if (userHetu == hetu) {
                    return u
                }
            }
        } else if (eppn != null) {
            userRepository.findAllWithAuthorities().filter { u -> u.eppn != null }.forEach { u ->
                if (u.eppn == eppn) {
                    return u
                }
            }
        }

        return null
    }

    private fun updateKouluttajaReferences(oldId: Long, newId: Long) {
        suoritusarviointiRepository.changeKouluttaja(oldId, newId)
        koejaksonKoulutussopimusRepository.changeKouluttaja(oldId, newId)
        koejaksonAloituskeskusteluRepository.changeKouluttaja(oldId, newId)
        koejaksonAloituskeskusteluRepository.changeEsimies(oldId, newId)
        koejaksonValiarviointiRepository.changeKouluttaja(oldId, newId)
        koejaksonValiarviointiRepository.changeEsimies(oldId, newId)
        koejaksonKehittamistoimenpiteetRepository.changeKouluttaja(oldId, newId)
        koejaksonKehittamistoimenpiteetRepository.changeEsimies(oldId, newId)
        koejaksonLoppukeskusteluRepository.changeKouluttaja(oldId, newId)
        koejaksonLoppukeskusteluRepository.changeEsimies(oldId, newId)
    }
}
