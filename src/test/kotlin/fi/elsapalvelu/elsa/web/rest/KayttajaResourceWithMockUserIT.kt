package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.UserRepository
import fi.elsapalvelu.elsa.security.ADMIN
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.USER
import fi.elsapalvelu.elsa.service.dto.UserDTO
import fi.elsapalvelu.elsa.service.mapper.UserMapper
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.security.test.context.support.WithMockUser
import java.time.Instant
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@WithMockUser(authorities = [ADMIN])
@SpringBootTest(classes = [ElsaBackendApp::class])
class KayttajaResourceWithMockUserIT {

    @Autowired
    private lateinit var userMapper: UserMapper

    @Autowired
    private lateinit var cacheManager: CacheManager

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)!!.clear()
        cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)!!.clear()
    }

    @BeforeEach
    fun initTest() {
        user = createEntity()
        user.apply {
            login = DEFAULT_LOGIN
            email = DEFAULT_EMAIL
            phoneNumber = DEFAULT_PHONE_NUMBER
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUserEquals() {
        equalsVerifier(User::class)
        val user1 = User(id = "id1")
        val user2 = User(id = user1.id)
        assertThat(user1).isEqualTo(user2)
        user2.id = "id2"
        assertThat(user1).isNotEqualTo(user2)
        user1.id = null
        assertThat(user1).isNotEqualTo(user2)
    }

    @Test
    fun testUserDTOtoUser() {
        val userDTO = UserDTO(
            id = DEFAULT_ID,
            login = DEFAULT_LOGIN,
            firstName = DEFAULT_FIRSTNAME,
            lastName = DEFAULT_LASTNAME,
            email = DEFAULT_EMAIL,
            phoneNumber = DEFAULT_PHONE_NUMBER,
            avatar = DEFAULT_AVATAR,
            activated = true,
            langKey = DEFAULT_LANGKEY,
            createdBy = DEFAULT_LOGIN,
            lastModifiedBy = DEFAULT_LOGIN,
            authorities = setOf(USER)
        )

        val user = userMapper.userDTOToUser(userDTO)
        assertNotNull(user)
        assertThat(user.id).isEqualTo(DEFAULT_ID)
        assertThat(user.login).isEqualTo(DEFAULT_LOGIN)
        assertThat(user.firstName).isEqualTo(DEFAULT_FIRSTNAME)
        assertThat(user.lastName).isEqualTo(DEFAULT_LASTNAME)
        assertThat(user.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(user.phoneNumber).isEqualTo(DEFAULT_PHONE_NUMBER)
        assertThat(user.avatar).isEqualTo(DEFAULT_AVATAR)
        assertThat(user.activated).isEqualTo(true)
        assertThat(user.langKey).isEqualTo(DEFAULT_LANGKEY)
        assertThat(user.createdBy).isNull()
        assertThat(user.createdDate).isNotNull
        assertThat(user.lastModifiedBy).isNull()
        assertThat(user.lastModifiedDate).isNotNull
        assertThat(user.authorities).extracting("name").containsExactly(USER)
    }

    @Test
    fun testUserToUserDTO() {
        user.id = DEFAULT_ID
        user.createdBy = DEFAULT_LOGIN
        user.createdDate = Instant.now()
        user.lastModifiedBy = DEFAULT_LOGIN
        user.lastModifiedDate = Instant.now()
        user.authorities = mutableSetOf(Authority(name = USER))

        val userDTO = userMapper.userToUserDTO(user)

        assertThat(userDTO.id).isEqualTo(DEFAULT_ID)
        assertThat(userDTO.login).isEqualTo(DEFAULT_LOGIN)
        assertThat(userDTO.firstName).isEqualTo(DEFAULT_FIRSTNAME)
        assertThat(userDTO.lastName).isEqualTo(DEFAULT_LASTNAME)
        assertThat(userDTO.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(userDTO.phoneNumber).isEqualTo(DEFAULT_PHONE_NUMBER)
        assertThat(userDTO.avatar).isEqualTo(DEFAULT_AVATAR)
        assertThat(userDTO.isActivated()).isEqualTo(true)
        assertThat(userDTO.langKey).isEqualTo(DEFAULT_LANGKEY)
        assertThat(userDTO.createdBy).isEqualTo(DEFAULT_LOGIN)
        assertThat(userDTO.createdDate).isEqualTo(user.createdDate)
        assertThat(userDTO.lastModifiedBy).isEqualTo(DEFAULT_LOGIN)
        assertThat(userDTO.lastModifiedDate).isEqualTo(user.lastModifiedDate)
        assertThat(userDTO.authorities).containsExactly(USER)
        assertThat(userDTO.toString()).isNotNull
    }

    @Test
    fun testAuthorityEquals() {
        val authorityA = Authority()
        assertThat(authorityA).isEqualTo(authorityA)
        assertThat(authorityA).isNotEqualTo(null)
        assertThat(authorityA).isNotEqualTo(Any())
        assertThat(authorityA.hashCode()).isEqualTo(31)
        assertThat(authorityA.toString()).isNotNull

        val authorityB = Authority()
        assertThat(authorityA.name).isEqualTo(authorityB.name)

        authorityB.name = ADMIN
        assertThat(authorityA).isNotEqualTo(authorityB)

        authorityA.name = USER
        assertThat(authorityA).isNotEqualTo(authorityB)

        authorityB.name = USER
        assertThat(authorityA).isEqualTo(authorityB)
        assertThat(authorityA.hashCode()).isEqualTo(authorityB.hashCode())
    }

    companion object {

        private const val DEFAULT_LOGIN = "johndoe"

        private const val DEFAULT_ID = "id1"

        private const val DEFAULT_EMAIL = "johndoe@localhost"

        private const val DEFAULT_PHONE_NUMBER = "1234567890"

        private val DEFAULT_AVATAR: ByteArray = createByteArray(1, "0")

        private const val DEFAULT_FIRSTNAME = "john"

        private const val DEFAULT_LASTNAME = "doe"

        private const val DEFAULT_LANGKEY = "en"

        private val DEFAULT_AUTHORITY = Authority(name = ERIKOISTUVA_LAAKARI)

        @JvmStatic
        fun createEntity(
            nimi: String = "$DEFAULT_FIRSTNAME $DEFAULT_LASTNAME",
            email: String? = RandomStringUtils.randomAlphabetic(5) + DEFAULT_EMAIL,
            authority: Authority = DEFAULT_AUTHORITY
        ): User {
            val names = nimi.split(" ")
            return User(
                login = DEFAULT_LOGIN + RandomStringUtils.randomAlphabetic(5),
                activated = true,
                email = email,
                phoneNumber = RandomStringUtils.randomAlphabetic(5) + DEFAULT_PHONE_NUMBER,
                avatar = DEFAULT_AVATAR,
                firstName = names.dropLast(1).joinToString(),
                lastName = names.last(),
                langKey = DEFAULT_LANGKEY,
                authorities = mutableSetOf(authority)
            )
        }
    }
}
