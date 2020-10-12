package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.User
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Spring Data JPA repository for the [User] entity.
 */
@Repository
interface UserRepository : JpaRepository<User, String> {

    fun findOneByLogin(login: String): Optional<User>

    @EntityGraph(attributePaths = ["authorities"])
    @Cacheable(cacheNames = [USERS_BY_LOGIN_CACHE])
    fun findOneWithAuthoritiesByLogin(login: String): Optional<User>

    fun findAllByLoginNot(pageable: Pageable, login: String): Page<User>

    companion object {

        const val USERS_BY_LOGIN_CACHE: String = "usersByLogin"

        const val USERS_BY_EMAIL_CACHE: String = "usersByEmail"
    }
}
