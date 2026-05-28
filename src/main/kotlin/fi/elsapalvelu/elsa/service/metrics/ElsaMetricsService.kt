package fi.elsapalvelu.elsa.service.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for all Elsa custom metric services.
 *
 * Subclasses register their meters (gauges, counters) in their `init {}` block
 * using the helpers below.  Because meters must be registered at startup to appear
 * in `/management/metrics`, every concrete subclass annotated with `@Service` **must**
 * also carry `@Lazy(false)`.  Without it, `spring.main.lazy-initialization=true`
 * (active in the dev profile) defers bean creation until first use, which means the
 * meter is never registered and never shows up — even after a login.
 *
 * ## Adding a new metric (example)
 * ```kotlin
 * @Service
 * @Lazy(false)
 * class MyMetricsService(registry: MeterRegistry) : ElsaMetricsService(registry) {
 *     private val myGauge = atomicGauge("my.metric.name", "Human-readable description")
 *     fun increment() { myGauge.incrementAndGet() }
 * }
 * ```
 * To export the new metric to CloudWatch, add its name to
 * `management.cloudwatch2.metrics.filter.allowed-names` in `application-prod.yml`
 * — no infra-live changes required.
 */
abstract class ElsaMetricsService(protected val registry: MeterRegistry) {

    protected fun atomicGauge(name: String, description: String): AtomicInteger {
        val value = AtomicInteger(0)
        Gauge.builder(name, value, AtomicInteger::toDouble)
            .description(description)
            .register(registry)
        return value
    }

    protected fun atomicGauge(name: String, description: String, vararg tags: String): AtomicInteger {
        val value = AtomicInteger(0)
        Gauge.builder(name, value, AtomicInteger::toDouble)
            .description(description)
            .tags(*tags)
            .register(registry)
        return value
    }

    protected fun counter(name: String, description: String, vararg tags: String): Counter {
        return Counter.builder(name)
            .description(description)
            .tags(*tags)
            .register(registry)
    }
}

