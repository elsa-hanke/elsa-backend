package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.User
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*
import org.springframework.transaction.annotation.Transactional


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

    @Query("select u from User u left join fetch u.authorities where u.id = :id")
    fun findByIdWithAuthorities(id: String): Optional<User>

    @Query("select u from User u left join fetch u.authorities")
    fun findAllWithAuthorities(): List<User>

    fun findOneByEmail(email: String): Optional<User>

    fun findOneByEppn(eppn: String): Optional<User>

    @Transactional
    @Modifying
    @Query("update User u set u.activeAuthority = :role where u.id = :userId and u.activeAuthority is null")
    fun setActiveAuthorityIfNull(userId: String, role: Authority)

    companion object {

        const val USERS_BY_LOGIN_CACHE: String = "usersByLogin"

        const val USERS_BY_EMAIL_CACHE: String = "usersByEmail"
    }
}
