@file:JvmName("SecurityUtils")

package fi.elsapalvelu.elsa.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken
import java.util.*

/**
 * Get the login of the current user.
 *
 * @return the login of the current user.
 */
fun getCurrentUserLogin(): Optional<String> =
    Optional.ofNullable(extractPrincipal(SecurityContextHolder.getContext().authentication))

fun extractPrincipal(authentication: Authentication?): String? {

    if (authentication == null) {
        return null
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    return when (val principal = authentication.principal) {
        is Saml2AuthenticationToken -> principal.name
        is String -> principal
        else -> null
    }
}

@Suppress("UNCHECKED_CAST")
fun getRolesFromClaims(claims: Map<String, Any>): Collection<String> {
    return claims.getOrDefault(
        "groups",
        claims.getOrDefault("roles", listOf<String>())
    ) as Collection<String>
}

fun mapRolesToGrantedAuthorities(roles: Collection<String>): List<GrantedAuthority> {
    return roles
        .filter { it.startsWith("ROLE_") }
        .map { SimpleGrantedAuthority(it) }
}
