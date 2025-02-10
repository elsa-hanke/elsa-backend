package fi.elsapalvelu.elsa.security

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.UserRepository
import org.springframework.core.log.LogMessage
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InternalAuthenticationServiceException
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
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Kouluttaja/vastuuhenkilö/opintohallinnon virkailija voi haluta katsoa erikoistujan tietoja, jolloin autentikaatiota
 * täytyy impersonoida. Toteutettu SwitchUserFilter pohjalta ja mukautettu toimimaan SAML kanssa.
 */
class ElsaSwitchUserFilter(
    private val opintooikeusRepository: OpintooikeusRepository,
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
                SecurityContextHolder.getContext().authentication = targetUser
                request.session.setAttribute("originalUrl", request.getParameter("originalUrl"))
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
            SecurityContextHolder.getContext().authentication = originalUser
            logger.debug(LogMessage.format("Set SecurityContextHolder to %s", originalUser))
            val principal =
                SecurityContextHolder.getContext().authentication.principal as Saml2AuthenticatedPrincipal
            SecurityLoggingWrapper.info("User with id ${principal.name} switched back to original session")
            val originalUrl = request.session.getAttribute("originalUrl")
            originalUrl?.let {
                response.sendRedirect(originalUrl as String)
            }
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
        val opintooikeusId = request.getParameter("opintooikeusId")
        val opintooikeus = opintooikeusRepository.findById(opintooikeusId.toLong()).orElseThrow {
            InternalAuthenticationServiceException("Erikoistuvaa lääkäriä ei löydy")
        }
        val yekOikeus = opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID
        val erikoistuvaLaakari = opintooikeus.erikoistuvaLaakari
        val principal =
            SecurityContextHolder.getContext().authentication.principal as Saml2AuthenticatedPrincipal

        val impersonatedRole = getImpersonatedRole(principal, opintooikeus)
        if (impersonatedRole == null) {
            SecurityLoggingWrapper.info("Denying access for user with id ${principal.name} to switch to user with id ${erikoistuvaLaakari?.kayttaja?.user?.id}")
            throw BadCredentialsException("Käyttäjällä ei oikeuksia katsella erikoistujan tietoja")
        } else {
            SecurityLoggingWrapper.info("User with id ${principal.name} switching to user with id ${erikoistuvaLaakari?.kayttaja?.user?.id}")
        }

        // Annetaan käyttäjälle uusi rooli ja tallennetaan nykyinen autentikaatio, jotta käyttäjä
        // saa poistuessa oman autentikaation takaisin
        val currentAuthentication: Authentication =
            SecurityContextHolder.getContext().authentication
        val switchAuthority: GrantedAuthority = SwitchUserGrantedAuthority(
            impersonatedRole,
            currentAuthentication
        )
        val currentPrincipal = currentAuthentication.principal as Saml2AuthenticatedPrincipal
        val newPrincipal = DefaultSaml2AuthenticatedPrincipal(
            erikoistuvaLaakari?.kayttaja?.user?.id,
            mapOf(
                "urn:oid:2.5.4.42" to listOf(erikoistuvaLaakari?.kayttaja?.user?.firstName),
                "urn:oid:2.5.4.4" to listOf(erikoistuvaLaakari?.kayttaja?.user?.lastName),
                "nameID" to currentPrincipal.attributes["nameID"],
                "nameIDFormat" to currentPrincipal.attributes["nameIDFormat"],
                "nameIDQualifier" to currentPrincipal.attributes["nameIDQualifier"],
                "nameIDSPQualifier" to currentPrincipal.attributes["nameIDSPQualifier"],
                "opintooikeusId" to listOf(opintooikeus.id),
                "yek" to listOf(yekOikeus)
            )
        )
        if (currentPrincipal.relyingPartyRegistrationId != null) {
            newPrincipal.relyingPartyRegistrationId = currentPrincipal.relyingPartyRegistrationId
        }
        return Saml2Authentication(
            newPrincipal,
            (currentAuthentication as Saml2Authentication).saml2Response,
            listOf(switchAuthority)
        )
    }

    // Vain saman yliopiston ja erikoisalan vastuuhenkilöt, kouluttajavaltuutuksen saaneet
    // henkilöt tai saman yliopiston opintohallinnon virkailijat voivat katsella erikoistujan tietoja.
    private fun getImpersonatedRole(
        principal: Saml2AuthenticatedPrincipal,
        opintooikeus: Opintooikeus
    ): String? {
        val userId = principal.name
        val kirjautunutKayttaja =
            kayttajaRepository.findOneByUserIdWithErikoisalat(userId)
                    .orElseThrow { EntityNotFoundException("Käyttäjä ei ole kirjautunut") }

        val authorityNames =
            userRepository.findByIdWithAuthorities(kirjautunutKayttaja.user?.id!!)
                .get().authorities.map { it.name }

        if (authorityNames.contains(VASTUUHENKILO) && kirjautunutKayttaja.yliopistotAndErikoisalat.find {
                it.erikoisala?.id == opintooikeus.erikoisala?.id &&
                    it.yliopisto?.id == opintooikeus.yliopisto?.id
            } != null) return ERIKOISTUVA_LAAKARI_IMPERSONATED

        if (authorityNames.contains(VASTUUHENKILO) && opintooikeus.erikoisala?.id == YEK_ERIKOISALA_ID
            && kirjautunutKayttaja.yliopistotAndErikoisalat.any {
                it.yliopisto?.id == opintooikeus.yliopisto?.id
                    && it.vastuuhenkilonTehtavat.map { t -> t.nimi }.contains(VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN) })
            return ERIKOISTUVA_LAAKARI_IMPERSONATED

        if (authorityNames.contains(OPINTOHALLINNON_VIRKAILIJA) && kirjautunutKayttaja.yliopistot.find {
                it.id == opintooikeus.yliopisto?.id
            } != null) return ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA

        if (kouluttajavaltuutusRepository.findByValtuuttajaOpintooikeusIdAndValtuutettuUserIdAndPaattymispaivaAfter(
                opintooikeus.id!!,
                kirjautunutKayttaja.user?.id!!,
                LocalDate.now().minusDays(1)
            ).isPresent
        ) return ERIKOISTUVA_LAAKARI_IMPERSONATED

        return null
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
