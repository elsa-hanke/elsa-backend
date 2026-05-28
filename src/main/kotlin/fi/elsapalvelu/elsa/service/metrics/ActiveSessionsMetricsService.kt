package fi.elsapalvelu.elsa.service.metrics

import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpSessionEvent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent
import org.springframework.security.web.session.HttpSessionDestroyedEvent
import org.springframework.stereotype.Service

/**
 * Tracks the number of actively authenticated user sessions
 * as a Micrometer gauge (`http.sessions.active`).
 *
 * ## Why not `HttpSessionCreatedEvent` + `HttpSessionDestroyedEvent`?
 *
 * Spring Security creates a **new anonymous session** immediately after a SAML SLO
 * (Suomi.fi) logout so it can write the logout response cookie.  Using plain session
 * lifecycle events therefore produces an incorrect count:
 *  - session destroyed (authenticated user logs out)  → counter −1  ✓
 *  - session created (anonymous post-logout session)  → counter +1  ✗  ← false positive
 *
 * ## Correct event pair
 *
 * | Event | When | Action |
 * |---|---|---|
 * | [InteractiveAuthenticationSuccessEvent] | User completes SAML / Haka / SSO login | `+1` |
 * | [HttpSessionDestroyedEvent] (authenticated only) | Session invalidated on logout *or* server-side timeout | `−1` |
 *
 * [HttpSessionDestroyedEvent] exposes the session's [org.springframework.security.core.context.SecurityContext]
 * list via `event.securityContexts`.  We only decrement when at least one context holds
 * an authentic (non-anonymous) authentication object, so anonymous-session destruction
 * (e.g. unauthenticated users, post-logout redirect sessions) is silently ignored.
 *
 * ## Prerequisites
 * [org.springframework.security.web.session.HttpSessionEventPublisher] must be registered
 * as a servlet listener.  This is done in [fi.elsapalvelu.elsa.config.WebConfigurer].
 */
@Service
@Lazy(false) // Gauges are registered in init{} — must be eagerly instantiated even when
             // spring.main.lazy-initialization=true (dev profile). Apply @Lazy(false) to
             // every ElsaMetricsService subclass for the same reason.
class ActiveSessionsMetricsService(registry: MeterRegistry) : ElsaMetricsService(registry) {

    private val log = LoggerFactory.getLogger(ActiveSessionsMetricsService::class.java)

    private val activeSessions = atomicGauge(
        name        = "http.sessions.active",
        description = "Number of currently active authenticated user sessions (proxy for logged-in users)"
    )

    /**
     * Increment when a user interactively completes authentication (SAML, Haka, etc.).
     *
     * [InteractiveAuthenticationSuccessEvent] is published by
     * [org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter]
     * which is the base of all SAML / form-login filters.  It fires exactly once per
     * successful interactive login — NOT on session restoration from cookie.
     */
    @EventListener
    fun onAuthenticationSuccess(event: InteractiveAuthenticationSuccessEvent) {
        val count = activeSessions.incrementAndGet()
        log.debug(
            "User authenticated ({}), active sessions={}",
            event.authentication.name,
            count
        )
    }

    /**
     * Decrement when a session that belonged to an authenticated user is destroyed.
     *
     * Covers both explicit logout (SAML SLO / local logout) and server-side session
     * timeout.  Anonymous sessions (pre-login or post-logout redirects) are ignored
     * so the counter never drifts below the true number of logged-in users.
     */
    @EventListener
    fun onSessionDestroyed(event: HttpSessionDestroyedEvent) {
        val wasAuthenticated = event.securityContexts.any { ctx ->
            val auth = ctx.authentication
            auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken
        }

        if (wasAuthenticated) {
            val count = activeSessions.updateAndGet { maxOf(0, it - 1) }
            log.debug(
                "Authenticated session destroyed: id={}, active sessions={}",
                (event.source as? HttpSessionEvent)?.session?.id,
                count
            )
        }
    }
}

