package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class KoejaksoUpdateActorTest {
    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @ParameterizedTest
    @EnumSource(KoejaksoUpdateActor::class)
    fun `an unauthenticated caller cannot select an actor`(actor: KoejaksoUpdateActor) {
        SecurityContextHolder.clearContext()
        assertThrows<AccessDeniedException> { actor.requireAuthority() }
    }

    @ParameterizedTest
    @EnumSource(KoejaksoUpdateActor::class)
    fun `only the selected actors authority is required`(actor: KoejaksoUpdateActor) {
        authenticate(listOf(authority(actor)))
        actor.requireAuthority()
    }

    @ParameterizedTest
    @EnumSource(KoejaksoUpdateActor::class)
    fun `other authorities do not grant the selected actor`(actor: KoejaksoUpdateActor) {
        authenticate(KoejaksoUpdateActor.values().filter { it != actor }.map(::authority))
        assertThrows<AccessDeniedException> { actor.requireAuthority() }
    }

    private fun authenticate(authorities: List<String>) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("test", "unused", authorities.map(::SimpleGrantedAuthority))
    }

    private fun authority(actor: KoejaksoUpdateActor) = when (actor) {
        KoejaksoUpdateActor.TRAINEE -> ERIKOISTUVA_LAAKARI
        KoejaksoUpdateActor.TRAINER -> KOULUTTAJA
        KoejaksoUpdateActor.REVIEWER -> OPINTOHALLINNON_VIRKAILIJA
        KoejaksoUpdateActor.APPROVER -> VASTUUHENKILO
    }
}
