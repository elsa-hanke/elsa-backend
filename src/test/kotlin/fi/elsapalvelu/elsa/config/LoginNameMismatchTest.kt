package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import fi.elsapalvelu.elsa.repository.kayttaja.VerificationTokenRepository
import fi.elsapalvelu.elsa.security.LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE
import fi.elsapalvelu.elsa.security.LoginNameMismatch
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.InvocationTargetException
import java.util.Optional
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class LoginNameMismatchTest {

    @Test
    fun `failed invitation retains names for display and next login clears them`() {
        val invited = User(id = "invited-user", firstName = "Aleksi", lastName = "Räisänen")
        val token = VerificationToken(id = "invitation-token", user = invited)
        val userRepository = mock<UserRepository>()
        val tokenRepository = mock<VerificationTokenRepository>()
        val userService = mock<UserService>()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key = SecretKeySpec(ByteArray(16), "AES")
        val failure = Exception(LoginException.VIRHEELLINEN_NIMI.name)
        whenever(tokenRepository.findById(token.id!!)).thenReturn(Optional.of(token))
        whenever(userRepository.findByIdWithAuthorities(invited.id!!)).thenReturn(Optional.of(invited))
        doAnswer { throw failure }.whenever(userService).createOrUpdateUserWithToken(
            invited, token, cipher, key, null, null, "Kari", "Räisänen"
        )
        val config = testSecurityConfiguration(
            userService = userService,
            userRepository = userRepository,
            verificationTokenRepository = tokenRepository
        )
        val findUser = SecurityConfiguration::class.java.getDeclaredMethod(
            "findLoginUser", Cipher::class.java, SecretKey::class.java,
            String::class.java, String::class.java, String::class.java, String::class.java
        ).apply { isAccessible = true }
        val request = MockHttpServletRequest().apply { setParameter("RelayState", token.id!!) }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        try {
            val beforeLogin = Instant.now()
            val thrown = assertThrows<InvocationTargetException> {
                findUser.invoke(config, cipher, key, null, null, "Kari", "Räisänen")
            }
            assertThat(thrown.cause).isSameAs(failure)
            val mismatch = request.session.getAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE) as LoginNameMismatch
            assertThat(mismatch.invitationName).isEqualTo("Aleksi Räisänen")
            assertThat(mismatch.authenticatedName).isEqualTo("Kari Räisänen")
            assertThat(mismatch.expiresAt).isBetween(beforeLogin.plusSeconds(300), Instant.now().plusSeconds(300))

            request.removeParameter("RelayState")
            val existing = User(id = "existing-user", firstName = "Kari", lastName = "Räisänen")
            whenever(userService.findExistingUser(cipher, key, null, null)).thenReturn(existing)
            assertThat(findUser.invoke(config, cipher, key, null, null, "Kari", "Räisänen"))
                .isSameAs(existing)
            assertThat(request.session.getAttribute(LOGIN_NAME_MISMATCH_SESSION_ATTRIBUTE)).isNull()
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }
}
