package fi.elsapalvelu.elsa.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class SecurityUtilsTest {

    @Test
    fun `getCurrentUserLogin should return empty when no authentication`() {
        SecurityContextHolder.clearContext()

        val result = getCurrentUserLogin()

        assertThat(result).isEmpty
    }

    @Test
    fun `getCurrentUserLogin should return username from string principal`() {
        val authentication = UsernamePasswordAuthenticationToken("testuser", "password")
        SecurityContextHolder.getContext().authentication = authentication

        val result = getCurrentUserLogin()

        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo("testuser")
    }


    @Test
    fun `extractPrincipal should return null when authentication is null`() {
        val result = extractPrincipal(null)

        assertThat(result).isNull()
    }

    @Test
    fun `extractPrincipal should return username from string principal`() {
        val authentication = UsernamePasswordAuthenticationToken("user123", "password")

        val result = extractPrincipal(authentication)

        assertThat(result).isEqualTo("user123")
    }

    @Test
    fun `getRolesFromClaims should extract roles from groups claim`() {
        val claims = mapOf("groups" to listOf("ROLE_ADMIN", "ROLE_USER"))

        val result = getRolesFromClaims(claims)

        assertThat(result).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER")
    }

    @Test
    fun `getRolesFromClaims should extract roles from roles claim when groups not present`() {
        val claims = mapOf("roles" to listOf("ROLE_KOULUTTAJA", "ROLE_ERIKOISTUVA_LAAKARI"))

        val result = getRolesFromClaims(claims)

        assertThat(result).containsExactlyInAnyOrder("ROLE_KOULUTTAJA", "ROLE_ERIKOISTUVA_LAAKARI")
    }

    @Test
    fun `getRolesFromClaims should prefer groups over roles when both present`() {
        val claims = mapOf(
            "groups" to listOf("ROLE_ADMIN"),
            "roles" to listOf("ROLE_USER")
        )

        val result = getRolesFromClaims(claims)

        assertThat(result).containsExactly("ROLE_ADMIN")
    }

    @Test
    fun `getRolesFromClaims should return empty list when no roles or groups`() {
        val claims = mapOf<String, Any>("other" to "value")

        val result = getRolesFromClaims(claims)

        assertThat(result).isEmpty()
    }

    @Test
    fun `mapRolesToGrantedAuthorities should filter and map roles starting with ROLE_`() {
        val roles = listOf("ROLE_ADMIN", "ROLE_USER", "OTHER_PERMISSION")

        val result = mapRolesToGrantedAuthorities(roles)

        assertThat(result).hasSize(2)
        assertThat(result).extracting("authority")
            .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER")
    }

    @Test
    fun `mapRolesToGrantedAuthorities should return empty list when no valid roles`() {
        val roles = listOf("PERMISSION_READ", "PERMISSION_WRITE")

        val result = mapRolesToGrantedAuthorities(roles)

        assertThat(result).isEmpty()
    }

    @Test
    fun `mapRolesToGrantedAuthorities should handle empty list`() {
        val roles = emptyList<String>()

        val result = mapRolesToGrantedAuthorities(roles)

        assertThat(result).isEmpty()
    }

    @Test
    fun `mapRolesToGrantedAuthorities should create SimpleGrantedAuthority instances`() {
        val roles = listOf("ROLE_ADMIN")

        val result = mapRolesToGrantedAuthorities(roles)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(SimpleGrantedAuthority::class.java)
        assertThat(result[0].authority).isEqualTo("ROLE_ADMIN")
    }

    @Test
    fun `mapRolesToGrantedAuthorities should handle all user roles`() {
        val roles = listOf(
            "ROLE_ERIKOISTUVA_LAAKARI",
            "ROLE_KOULUTTAJA",
            "ROLE_VASTUUHENKILO",
            "ROLE_VIRKAILIJA",
            "ROLE_TEKNINEN_PAAKAYTTAJA"
        )

        val result = mapRolesToGrantedAuthorities(roles)

        assertThat(result).hasSize(5)
        assertThat(result.map { it.authority }).containsExactlyInAnyOrder(
            "ROLE_ERIKOISTUVA_LAAKARI",
            "ROLE_KOULUTTAJA",
            "ROLE_VASTUUHENKILO",
            "ROLE_VIRKAILIJA",
            "ROLE_TEKNINEN_PAAKAYTTAJA"
        )
    }
}

