package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import io.undertow.util.BadRequestException
import org.springframework.core.log.LogMessage
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper
import java.time.LocalDate
import javax.persistence.EntityNotFoundException
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Kouluttaja/vastuuhenkilö voi haluta katsoa erikoistujan tietoja, jolloin autentikaatiota
 * täytyy impersonoida. Toteutettu SwitchUserFilter pohjalta ja mukautettu toimimaan SAML kanssa.
 */
class ElsaSwitchUserFilter(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val userRepository: UserRepository,
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository
) : OncePerRequestFilter() {

    private val successHandler: AuthenticationSuccessHandler =
        SimpleUrlAuthenticationSuccessHandler()
    private val failureHandler: AuthenticationFailureHandler =
        SimpleUrlAuthenticationFailureHandler()
    private val switchUserMatcher = createMatcher("/api/login/impersonate")
    private val exitUserMatcher = createMatcher("/api/logout/impersonate")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (requiresSwitchUser(request)) {
            try {
                val targetUser: Authentication = attemptSwitchUser(request)
                val context = SecurityContextHolder.createEmptyContext()
                context.authentication = targetUser
                SecurityContextHolder.setContext(context)
                logger.debug(LogMessage.format("Set SecurityContextHolder to %s", targetUser))
                successHandler.onAuthenticationSuccess(request, response, targetUser)
            } catch (ex: AuthenticationException) {
                logger.debug("Failed to switch user", ex)
                failureHandler.onAuthenticationFailure(request, response, ex)
            }
            return
        }

        if (requiresExitUser(request)) {
            val originalUser: Authentication = attemptExitUser()
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = originalUser
            SecurityContextHolder.setContext(context)
            logger.debug(LogMessage.format("Set SecurityContextHolder to %s", originalUser))
            val principal =
                SecurityContextHolder.getContext().authentication.principal as Saml2AuthenticatedPrincipal
            SecurityLoggingWrapper.info("User with id ${principal.name} switched back to original session")
            successHandler.onAuthenticationSuccess(request, response, originalUser)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun requiresSwitchUser(request: HttpServletRequest?): Boolean {
        return switchUserMatcher.matches(request)
    }

    private fun requiresExitUser(request: HttpServletRequest?): Boolean {
        return this.exitUserMatcher.matches(request)
    }

    private fun attemptSwitchUser(request: HttpServletRequest): Authentication {
        val erikoistuvaLaakariId = request.getParameter("erikoistuvaLaakariId")
        val erikoistuvaLaakari =
            erikoistuvaLaakariRepository.findByIdWithOpintooikeudet(erikoistuvaLaakariId.toLong())
                ?: throw IllegalArgumentException("Erikoistuvaa lääkäriä ei löydy")
        val principal =
            SecurityContextHolder.getContext().authentication.principal as Saml2AuthenticatedPrincipal

        if (!onOikeus(principal, erikoistuvaLaakari)) {
            SecurityLoggingWrapper.info("Denying access for user with id ${principal.name} to switch to user with id ${erikoistuvaLaakari.kayttaja?.user?.id}")
            throw BadRequestException("Käyttäjällä ei oikeuksia katsella erikoistujan tietoja")
        } else {
            SecurityLoggingWrapper.info("User with id ${principal.name} switching to user with id ${erikoistuvaLaakari.kayttaja?.user?.id}")
        }

        // Annetaan käyttäjälle uusi rooli ja tallennetaan nykyinen autentikaatio, jotta käyttäjä
        // saa poistuessa oman autentikaation takaisin
        val currentAuthentication: Authentication =
            SecurityContextHolder.getContext().authentication
        val switchAuthority: GrantedAuthority = SwitchUserGrantedAuthority(
            ERIKOISTUVA_LAAKARI_IMPERSONATED,
            currentAuthentication
        )
        val currentPrincipal = currentAuthentication.principal as Saml2AuthenticatedPrincipal
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(
            erikoistuvaLaakari.kayttaja?.user?.id,
            mapOf(
                "urn:oid:2.5.4.42" to listOf(erikoistuvaLaakari.kayttaja?.user?.firstName),
                "urn:oid:2.5.4.4" to listOf(erikoistuvaLaakari.kayttaja?.user?.lastName),
                "nameID" to currentPrincipal.attributes["nameID"],
                "nameIDFormat" to currentPrincipal.attributes["nameIDFormat"],
                "nameIDQualifier" to currentPrincipal.attributes["nameIDQualifier"],
                "nameIDSPQualifier" to currentPrincipal.attributes["nameIDSPQualifier"]
            )
        )
        newPrincipal.relyingPartyRegistrationId = currentPrincipal.relyingPartyRegistrationId
        return Saml2Authentication(
            newPrincipal,
            (currentAuthentication as Saml2Authentication).saml2Response,
            listOf(switchAuthority)
        )
    }

    // Vain saman yliopiston ja erikoisalan vastuuhenkilöt tai kouluttajavaltuutuksen saaneet
    // henkilöt voivat katsella erikoistujan tietoja
    private fun onOikeus(
        principal: Saml2AuthenticatedPrincipal,
        erikoistuvaLaakari: ErikoistuvaLaakari
    ): Boolean {
        val kirjautunutKayttaja = kayttajaRepository.findOneByUserIdWithErikoisalat(principal.name)
            .orElseThrow { EntityNotFoundException("Käyttäjä ei ole kirjautunut") }

        if (kirjautunutKayttaja.yliopistotAndErikoisalat.none { it.erikoisala?.id == erikoistuvaLaakari.getOpintooikeusKaytossa()?.erikoisala?.id && it.yliopisto?.id == erikoistuvaLaakari.getOpintooikeusKaytossa()?.yliopisto?.id }) {
            return false
        }

        val authorities =
            userRepository.findByIdWithAuthorities(kirjautunutKayttaja.user?.id!!).get().authorities

        if (authorities.map { it.name }.contains(VASTUUHENKILO)) {
            return true
        }

        return kouluttajavaltuutusRepository.findByValtuuttajaKayttajaUserIdAndValtuutettuUserIdAndPaattymispaivaAfter(
            erikoistuvaLaakari.kayttaja?.user?.id!!,
            kirjautunutKayttaja.user?.id!!,
            LocalDate.now().minusDays(1)
        ).isPresent
    }

    private fun attemptExitUser(): Authentication {
        val current = SecurityContextHolder.getContext().authentication
        return getSourceAuthentication(current)
            ?: throw AuthenticationCredentialsNotFoundException("Alkuperäistä käyttäjää ei löytynyt")
    }

    private fun getSourceAuthentication(current: Authentication): Authentication? {
        var original: Authentication? = null
        val authorities = current.authorities
        for (auth in authorities) {
            if (auth is SwitchUserGrantedAuthority) {
                original = auth.source
            }
        }
        return original
    }

    companion object {
        private fun createMatcher(pattern: String): RequestMatcher {
            return AntPathRequestMatcher(pattern, "GET", true, UrlPathHelper())
        }
    }
}
