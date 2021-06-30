package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.config.ANONYMOUS_USER
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.DEFAULT_LANGUAGE
import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.dto.UserDTO
import org.keycloak.admin.client.Keycloak
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Principal
import java.util.*
import javax.servlet.http.HttpServletRequest


@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val authorityRepository: AuthorityRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val erikoisalaRepository: ErikoisalaRepository,
    private val cacheManager: CacheManager,
    private val keycloak: Keycloak
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getAllManagedUsers(pageable: Pageable): Page<UserDTO> =
        userRepository.findAllByLoginNot(pageable, ANONYMOUS_USER).map { UserDTO(it) }

    @Transactional(readOnly = true)
    fun getUserWithAuthoritiesByLogin(login: String): Optional<User> =
        userRepository.findOneWithAuthoritiesByLogin(login)

    /**
     * @return a list of all the authorities
     */
    @Transactional(readOnly = true)
    fun getAuthorities() =
        authorityRepository.findAll().asSequence().map { it.name }.filterNotNullTo(mutableListOf())

    /**
     * Luodaan erikoistuva lääkäri ja käyttäjä entiteetit erikoistuvalle lääkärille jos niitä ei vielä ole.
     */
    private fun handleNewErikoistuvaLaakari(user: User) {
        if (
            user.authorities.contains(Authority(ERIKOISTUVA_LAAKARI)) &&
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!) == null
        ) {
            val kayttaja = kayttajaRepository.save(
                Kayttaja(
                    user = user,
                )
            )
            // TODO: erikoisalan valinta opinto-oikeuden mukaan
            val erikoisala = erikoisalaRepository.findByIdOrNull(1)
            erikoisala?.let {
                erikoistuvaLaakariRepository.save(
                    ErikoistuvaLaakari(
                        kayttaja = kayttaja,
                        erikoisala = erikoisala,
                    )
                )
            } ?: erikoistuvaLaakariRepository.save(
                ErikoistuvaLaakari(
                    kayttaja = kayttaja,
                )
            )
        }
    }

    /**
     * Returns the user from an OAuth 2.0 login or resource server with JWT.
     * Synchronizes the user in the local repository.
     *
     * @param authToken the authentication token.
     * @return the user from the authentication.
     */
    @Transactional
    fun getUserFromAuthentication(authToken: Saml2Authentication): UserDTO {
        val principal = authToken.principal as Saml2AuthenticatedPrincipal

        val user = User()
        user.id = principal.name
        user.firstName = principal.getFirstAttribute("urn:oid:2.5.4.42")
        user.lastName = principal.getFirstAttribute("urn:oid:2.5.4.4")
        user.authorities = authToken.authorities.map(GrantedAuthority::getAuthority)
            .map { Authority(name = it) }
            .toMutableSet()
        handleNewErikoistuvaLaakari(user) // TODO: move to erikoistuva lääkäri add function
        return UserDTO(user)

        /*var user = getUser(attributes)
        user.authorities = authToken.authorities.asSequence()
            .map(GrantedAuthority::getAuthority)
            .map { Authority(name = it) }
            .toMutableSet()

        user = syncUserWithIdP(attributes, user)

        handleNewErikoistuvaLaakari(user)

        return UserDTO(user)*/
    }

    fun getAuthenticatedUser(principal: Principal?): UserDTO {
        if (principal is Saml2Authentication) {
            return getUserFromAuthentication(principal)
        } else {
            throw RuntimeException("Käyttäjä ei ole kirjautunut")
        }
    }

    fun updateUserAuthorities(
        principal: Principal?,
        request: HttpServletRequest
    ) {
        // TODO: tarkista käyttöoikeus opintotietojärjestelmästä

        // Lisätään käyttäjä erikoistuvat lääkärit ryhmään
        val user = getAuthenticatedUser(principal)
        val realmResource = keycloak.realm("elsa")
        realmResource.groups().groups().find { it.name == "Erikoistuvat lääkärit" }?.let {
            val userResource = realmResource.users().get(user.id)
            userResource.joinGroup(it.id)
        }

        // Vaaditaan, jotta rooli päivittyy erikoistuvalle lääkärille
        request.session.invalidate()
    }

    private fun clearUserCaches(user: User) {
        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)?.evict(user.login!!)
        if (user.email != null) {
            cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)?.evict(user.email!!)
        }
    }

    companion object {

        @JvmStatic
        private fun getUser(details: Map<String, Any>): User {
            val user = User()
            // handle resource server JWT, where sub claim is email and uid is ID
            if (details["uid"] != null) {
                user.id = details["uid"] as String
                user.login = details["sub"] as String
            } else {
                user.id = details["sub"] as String
            }
            if (details["preferred_username"] != null) {
                user.login = (details["preferred_username"] as String).toLowerCase()
            } else if (user.login == null) {
                user.login = user.id
            }
            if (details["given_name"] != null) {
                user.firstName = details["given_name"] as String
            }
            if (details["family_name"] != null) {
                user.lastName = details["family_name"] as String
            }
            if (details["email_verified"] != null) {
                user.activated = details["email_verified"] as Boolean
            }
            if (details["email"] != null) {
                user.email = (details["email"] as String).toLowerCase()
            } else {
                user.email = details["sub"] as String
            }
            if (details["langKey"] != null) {
                user.langKey = details["langKey"] as String
            } else if (details["locale"] != null) {
                // trim off country code if it exists
                var locale = details["locale"] as String
                if (locale.contains("_")) {
                    locale = locale.substring(0, locale.indexOf("_"))
                } else if (locale.contains("-")) {
                    locale = locale.substring(0, locale.indexOf("-"))
                }
                user.langKey = locale.toLowerCase()
            } else {
                // set langKey to default if not specified by IdP
                user.langKey = DEFAULT_LANGUAGE
            }
            user.activated = true
            return user
        }
    }
}
