package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.*
import org.springframework.context.annotation.Import
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.util.CollectionUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.filter.CorsFilter
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport
import java.nio.charset.StandardCharsets
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
    private val relyingPartyRegistrationResolver: ElsaRelyingPartyRegistrationResolver
) : WebSecurityConfigurerAdapter() {

    override fun configure(web: WebSecurity?) {
        web!!.ignoring()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .antMatchers("/h2-console/**")
            .antMatchers("/swagger-ui/index.html")
            .antMatchers("/test/**")
    }

    @Throws(Exception::class)
    public override fun configure(http: HttpSecurity) {
        val withHttpOnlyFalse = CookieCsrfTokenRepository.withHttpOnlyFalse()
        val authenticationProvider =
            OpenSamlAuthenticationProvider() // TODO: update to OpenSaml4AuthenticationProvider when opensaml-api updates to 4.1.1 in maven
        authenticationProvider.setResponseAuthenticationConverter(authenticationConverter())
        withHttpOnlyFalse.setCookieDomain(applicationProperties.getCsrf().cookie.domain)
        http
            .csrf()
            .csrfTokenRepository(withHttpOnlyFalse)
            .and()
            .addFilterBefore(corsFilter, CsrfFilter::class.java)
            .exceptionHandling()
            .authenticationEntryPoint(problemSupport)
            .accessDeniedHandler(problemSupport)
            .and()
            .headers()
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
            .antMatchers("/api/auth-info").permitAll()
            .antMatchers("/api/erikoistuva-laakari/kayttooikeushakemus").authenticated()
            .antMatchers("/api/erikoistuva-laakari/**").hasAuthority(ERIKOISTUVA_LAAKARI)
            .antMatchers("/api/kouluttaja/**").hasAuthority(KOULUTTAJA)
            .antMatchers("/api/vastuuhenkilo/**").hasAuthority(VASTUUHENKILO)
            .antMatchers("/api/**").authenticated()
            .antMatchers("/management/health").permitAll()
            .antMatchers("/management/info").permitAll()
            .antMatchers("/management/prometheus").permitAll()
            .antMatchers("/management/**").hasAuthority(ADMIN)
            .and()
            .saml2Login()
            .authenticationConverter(
                Saml2AuthenticationTokenConverter(
                    relyingPartyRegistrationResolver
                )
            )
            .authenticationManager(ProviderManager(authenticationProvider))
            .defaultSuccessUrl("/", true) // TODO: ohjaa pyydettyyn front end näkymään
            .failureUrl("/") // TODO: Ohjaa kirjautumiseen tai front end näkymään
    }

    fun authenticationConverter(): Converter<OpenSamlAuthenticationProvider.ResponseToken, AbstractAuthenticationToken> {
        return Converter { responseToken ->
            convertAuthentication(responseToken)
        }
    }

    fun convertAuthentication(responseToken: OpenSamlAuthenticationProvider.ResponseToken): Saml2Authentication? {
        val token: Saml2Authentication = OpenSamlAuthenticationProvider
            .createDefaultResponseAuthenticationConverter()
            .convert(responseToken) as Saml2Authentication
        val principal = token.principal as Saml2AuthenticatedPrincipal
        val hetu = principal.attributes["urn:oid:1.2.246.21"]?.get(0)
        val firstName = principal.attributes["urn:oid:2.5.4.42"]?.get(0) as String
        val lastName = principal.attributes["urn:oid:2.5.4.4"]?.get(0) as String

        val response = responseToken.response
        val assertion = CollectionUtils.firstElement(response.assertions)
        val nameID = assertion.subject.nameID
        principal.attributes["nameID"] = mutableListOf(nameID.value) as List<Any>?
        principal.attributes["nameIDFormat"] = mutableListOf(nameID.format) as List<Any>?
        principal.attributes["nameIDQualifier"] = mutableListOf(nameID.nameQualifier) as List<Any>?
        principal.attributes["nameIDSPQualifier"] =
            mutableListOf(nameID.spNameQualifier) as List<Any>?

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

        // Check invited user
        if (verificationToken != null) {
            verificationTokenRepository.findById(verificationToken)
                .ifPresent {
                    val tokenUser = userRepository.findByIdWithAuthorities(it.user?.id!!).get()

                    val existingUser = findExistingUser(cipher, originalKey, hetu)

                    // If user with same hetu exists, merge users
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
                        val ciphertext =
                            cipher.doFinal(hetu.toString().toByteArray(StandardCharsets.UTF_8))

                        tokenUser.hetu = ciphertext
                        tokenUser.initVector = iv
                        tokenUser.firstName = firstName
                        tokenUser.lastName = lastName

                        userRepository.save(tokenUser)
                        verificationTokenRepository.delete(it)
                    }
                }
        }

        val existingUser = findExistingUser(cipher, originalKey, hetu)
        if (existingUser != null) {
            return Saml2Authentication(
                DefaultSaml2AuthenticatedPrincipal(existingUser.id, principal.attributes),
                token.saml2Response,
                existingUser.authorities.map { SimpleGrantedAuthority(it.name) }
            )
        }

        cipher.init(Cipher.ENCRYPT_MODE, originalKey)
        val params = cipher.parameters
        val iv = params.getParameterSpec(IvParameterSpec::class.java).iv
        val ciphertext =
            cipher.doFinal(hetu.toString().toByteArray(StandardCharsets.UTF_8))

        // New user
        var user = User(
            firstName = firstName,
            lastName = lastName,
            hetu = ciphertext,
            initVector = iv,
            login = UUID.randomUUID().toString(),
            activated = true
        )
        user = userRepository.save(user)

        return Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, principal.attributes),
            token.saml2Response,
            mutableListOf()
        )
    }

    private fun findExistingUser(cipher: Cipher, originalKey: SecretKey, hetu: Any?): User? {
        userRepository.findAllWithAuthorities().filter { u -> u.hetu != null }.forEach { u ->
            cipher.init(Cipher.DECRYPT_MODE, originalKey, IvParameterSpec(u.initVector))
            val userHetu = String(cipher.doFinal(u.hetu), StandardCharsets.UTF_8)
            if (userHetu == hetu) {
                return u
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
