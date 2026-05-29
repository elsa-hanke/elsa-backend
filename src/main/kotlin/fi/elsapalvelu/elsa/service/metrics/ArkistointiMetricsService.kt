package fi.elsapalvelu.elsa.service.metrics

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Lazy(false)
class ArkistointiMetricsService(registry: MeterRegistry) : ElsaMetricsService(registry) {

    val activeArkistointiOperations = atomicGauge(
        "arkistointi.active",
        "Number of archiving operations (muodostaSahke / laheta) currently executing"
    )

    fun recordSuccess(yliopisto: YliopistoEnum, caseType: CaseType) {
        counter("arkistointi.requests.total", "Total completed archiving operations", "yliopisto", yliopisto.name, "caseType", caseType.value).increment()
    }

    fun recordError(yliopisto: YliopistoEnum, caseType: CaseType) {
        counter("arkistointi.errors.total", "Total failed archiving operations", "yliopisto", yliopisto.name, "caseType", caseType.value).increment()
    }
}

