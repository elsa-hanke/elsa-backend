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

@Service
@Lazy(false)
class ActiveSessionsMetricsService(registry: MeterRegistry) : ElsaMetricsService(registry) {

    private val log = LoggerFactory.getLogger(ActiveSessionsMetricsService::class.java)

    private val activeSessions = atomicGauge(
        name        = "http.sessions.active",
        description = "Number of currently active authenticated user sessions (proxy for logged-in users)"
    )

    private val totalSessions = counter(
        "http.sessions.total",
        "Total number of successful authenticated logins since application start"
    )

    @EventListener
    fun onAuthenticationSuccess(event: InteractiveAuthenticationSuccessEvent) {
        val count = activeSessions.incrementAndGet()
        totalSessions.increment()
        log.debug("User authenticated ({}), active sessions={}, total logins={}", event.authentication.name, count, totalSessions.count())
    }

    @EventListener
    fun onSessionDestroyed(event: HttpSessionDestroyedEvent) {
        val wasAuthenticated = event.securityContexts.any { ctx ->
            val auth = ctx.authentication
            auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken
        }

        if (wasAuthenticated) {
            val count = activeSessions.updateAndGet { maxOf(0, it - 1) }
            log.debug("Authenticated session destroyed: id={}, active sessions={}", (event.source as? HttpSessionEvent)?.session?.id, count)
        }
    }
}

