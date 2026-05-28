package fi.elsapalvelu.elsa.service.metrics

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * Tracks archiving (arkistointi) workload and volume in real time.
 *
 * Gauges
 * ──────
 * arkistointi.active                      – number of archiving operations currently in flight
 *                                           (muodostaSahke + laheta).  Shows peak concurrency
 *                                           and helps size thread-pool capacity.
 *
 * Counters  (tagged by yliopisto + caseType so any monitoring stack can slice/dice)
 * ────────────────────────────────────────────────────────────────────────────────
 * arkistointi.requests.total              – successfully completed archiving calls.
 *                                           Use a per-day rate to answer "how many items are
 *                                           archived daily?" – exactly the daily-volume insight
 *                                           requested.
 * arkistointi.errors.total                – failed archiving calls.
 *                                           A rising error rate can trigger an alert before the
 *                                           engineering team notices in logs.
 */
@Service
@Lazy(false)
class ArkistointiMetricsService(registry: MeterRegistry) : ElsaMetricsService(registry) {

    /** Number of archiving operations (package-build + send) currently executing. */
    val activeArkistointiOperations = atomicGauge(
        "arkistointi.active",
        "Number of archiving operations (muodostaSahke / laheta) currently executing"
    )

    /**
     * Records one completed archiving cycle tagged by university and case type.
     * Call this after a successful `laheta` call in ArkistointiService.
     */
    fun recordSuccess(yliopisto: YliopistoEnum, caseType: CaseType) {
        counter(
            "arkistointi.requests.total",
            "Total completed archiving operations",
            "yliopisto", yliopisto.name, "caseType", caseType.value
        ).increment()
    }

    /**
     * Records one failed archiving cycle tagged by university and case type.
     * Call this inside the catch block that wraps `laheta` in ArkistointiService.
     */
    fun recordError(yliopisto: YliopistoEnum, caseType: CaseType) {
        counter(
            "arkistointi.errors.total",
            "Total failed archiving operations",
            "yliopisto", yliopisto.name, "caseType", caseType.value
        ).increment()
    }
}

