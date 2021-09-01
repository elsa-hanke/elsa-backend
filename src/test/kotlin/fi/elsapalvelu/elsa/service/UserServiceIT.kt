package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ANONYMOUS_USER
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.ANONYMOUS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertNotNull

private const val DEFAULT_LOGIN = "johndoe"
private const val DEFAULT_EMAIL = "johndoe@localhost"
private const val DEFAULT_FIRSTNAME = "john"
private const val DEFAULT_LASTNAME = "doe"
private const val DEFAULT_LANGKEY = "dummy"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class UserServiceIT {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    private lateinit var user: User

    private lateinit var userDetails: MutableMap<String, List<Any>>

    @BeforeEach
    fun init() {
        user = User(
            login = DEFAULT_LOGIN,
            activated = true,
            email = DEFAULT_EMAIL,
            firstName = DEFAULT_FIRSTNAME,
            lastName = DEFAULT_LASTNAME,
            langKey = DEFAULT_LANGKEY
        )
        userDetails = mutableMapOf(
            "urn:oid:2.5.4.42" to listOf(DEFAULT_FIRSTNAME),
            "urn:oid:2.5.4.4" to listOf(DEFAULT_LASTNAME)
        )
    }

    @Test
    @Transactional
    fun assertThatAnonymousUserIsNotGet() {
        user.id = ANONYMOUS_USER
        user.login = ANONYMOUS_USER
        if (!userRepository.findOneByLogin(ANONYMOUS_USER).isPresent) {
            userRepository.saveAndFlush(user)
        }
        val pageable = PageRequest.of(0, userRepository.count().toInt())
        val allManagedUsers = userService.getAllManagedUsers(pageable)
        assertNotNull(allManagedUsers)
        assertThat(
            allManagedUsers.content.stream()
                .noneMatch { user -> ANONYMOUS_USER == user.login }
        )
            .isTrue
    }

    @Test
    @Transactional
    fun testDefaultUserDetails() {
        val authentication = createMockSaml2AuthenticationToken(userDetails)
        val userDTO = userService.getUserFromAuthentication(authentication)

        assertThat(userDTO.firstName).isEqualTo(DEFAULT_FIRSTNAME)
        assertThat(userDTO.lastName).isEqualTo(DEFAULT_LASTNAME)
        assertThat(userDTO.authorities).contains(ANONYMOUS)
    }

    @Test
    @Transactional
    fun testUserDetailsWithUsername() {
        userDetails["urn:oid:2.5.4.42"] = listOf("test")
        val authentication = createMockSaml2AuthenticationToken(userDetails)
        val userDTO = userService.getUserFromAuthentication(authentication)
        assertThat(userDTO.firstName).isEqualTo("test")
    }

    private fun createMockSaml2AuthenticationToken(userDetails: Map<String, List<Any>?>): Saml2Authentication {
        val authorities = listOf(SimpleGrantedAuthority(ANONYMOUS))
        val usernamePasswordAuthenticationToken =
            UsernamePasswordAuthenticationToken(ANONYMOUS_USER, ANONYMOUS_USER, authorities)
        usernamePasswordAuthenticationToken.details = userDetails

        return Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal("sub", userDetails),
            "test",
            authorities
        )
    }
}
