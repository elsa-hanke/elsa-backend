package fi.elsapalvelu.elsa.service.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.atomic.AtomicInteger

abstract class ElsaMetricsService(protected val registry: MeterRegistry) {

    protected fun atomicGauge(name: String, description: String): AtomicInteger {
        val value = AtomicInteger(0)
        Gauge.builder(name, value, AtomicInteger::toDouble).description(description).register(registry)
        return value
    }

    protected fun atomicGauge(name: String, description: String, vararg tags: String): AtomicInteger {
        val value = AtomicInteger(0)
        Gauge.builder(name, value, AtomicInteger::toDouble).description(description).tags(*tags).register(registry)
        return value
    }

    protected fun counter(name: String, description: String, vararg tags: String): Counter {
        return Counter.builder(name).description(description).tags(*tags).register(registry)
    }
}

