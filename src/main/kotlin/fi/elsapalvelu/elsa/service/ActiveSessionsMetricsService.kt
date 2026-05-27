package fi.elsapalvelu.elsa.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpSessionEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.security.web.session.HttpSessionCreatedEvent
import org.springframework.security.web.session.HttpSessionDestroyedEvent
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

/**
 * Micrometer gauge that tracks the number of active HTTP sessions (i.e. logged-in users).
 *
 * Listens to Spring Security session lifecycle events which are published by
 * [org.springframework.security.web.session.HttpSessionEventPublisher].
 * That publisher must be registered as a servlet listener – see [fi.elsapalvelu.elsa.config.WebConfigurer]
 * or a dedicated `@Bean`.
 *
 * The gauge is exported to CloudWatch (production) under the metric name
 * `http.sessions.active` in the `ElsaBackend` namespace.
 */
@Service
class ActiveSessionsMetricsService(registry: MeterRegistry) {

    private val log = LoggerFactory.getLogger(ActiveSessionsMetricsService::class.java)

    private val activeSessions = AtomicInteger(0)

    init {
        // Note: the "application" tag is intentionally omitted here because
        // management.metrics.tags.application already adds it as a global common tag.
        // Adding it explicitly a second time would create a duplicate CloudWatch
        // dimension (application=elsa-backend AND application=elsaBackend), which
        // AWS rejects with InvalidParameterValue.
        Gauge.builder("http.sessions.active", activeSessions, AtomicInteger::toDouble)
            .description("Number of currently active HTTP sessions (proxy for logged-in users)")
            .register(registry)
    }

    @EventListener
    fun onSessionCreated(event: HttpSessionCreatedEvent) {
        val count = activeSessions.incrementAndGet()
        log.debug("Session created: id={}, active={}", event.session.id, count)
    }

    @EventListener
    fun onSessionDestroyed(event: HttpSessionDestroyedEvent) {
        val count = activeSessions.updateAndGet { maxOf(0, it - 1) }
        log.debug("Session destroyed: id={}, active={}", (event.source as? HttpSessionEvent)?.session?.id, count)
    }
}

