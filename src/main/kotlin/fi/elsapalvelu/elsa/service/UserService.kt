package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.VerificationToken
import fi.elsapalvelu.elsa.service.dto.OmatTiedotDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import java.security.Principal
import javax.crypto.Cipher
import javax.crypto.SecretKey

interface UserService {

    fun getAllManagedUsers(pageable: Pageable): Page<UserDTO>

    fun getUserFromAuthentication(authToken: Saml2Authentication): UserDTO

    fun getUser(userId: String): UserDTO

    fun getAuthenticatedUser(principal: Principal?): UserDTO

    fun createUser(
        cipher: Cipher, originalKey: SecretKey, hetu: String?, firstName: String, lastName: String
    ): UserDTO

    fun updateUserDetails(
        omatTiedotDTO: OmatTiedotDTO, userId: String
    ): UserDTO

    fun updateEmail(email: String, userId: String)

    fun createOrUpdateUserWithToken(
        tokenUser: User?,
        token: VerificationToken,
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        eppn: String?,
        firstName: String,
        lastName: String
    )

    fun existsByEmail(email: String): Boolean

    fun existsByEppn(eppn: String): Boolean

    fun findExistingUser(
        cipher: Cipher,
        originalKey: SecretKey,
        hetu: String?,
        eppn: String?
    ): User?

    fun updateRooli(rooli: String, userId: String)

    fun updateKouluttajaReferences(oldId: Long, newId: Long)

    fun updateAvoinKouluttajaReferences(oldId: Long, newId: Long)

    fun delete(id: String)
}
