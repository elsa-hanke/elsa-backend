package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.UserRepository
import org.mockito.Mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*
import kotlin.test.Test

val mockUser = mock<User> {
    on { id } doReturn  "test-user-id"
    on { authorities } doReturn mutableSetOf<Authority>(Authority("ROLE_ERIKOISTUVA_LAAKARI"))
}

val erikoistuvaLaakari = ErikoistuvaLaakari(
    id = 1L,
    kayttaja = mock {
        on { user } doReturn mockUser
    })


class ErikoistuvaLaakariCleanupServiceImplTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private val erikoistuvaLaakariCleanupService = ErikoistuvaLaakariCleanupServiceImpl(userRepository)

    @Test
    fun testClearErikoistuvaLaakari() {
        whenever(userRepository.findByIdWithAuthorities("test-user-id"))
            .thenReturn(Optional.of(mockUser))

        erikoistuvaLaakariCleanupService.clearErikoistuvaLaakari(erikoistuvaLaakari)


    }


}
