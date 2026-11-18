package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiAdapter
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/** Approval needs a synchronous adapter or an async queue route, even without a local worker. */
@Component
@Lazy(false)
class ArkistointiRouteValidator(
    configuration: ArkistointiConfigurationProvider,
    synchronousAdapters: List<SynchronousArkistointiAdapter>
) {
    init {
        val synchronousUniversities = synchronousAdapters.map { it.yliopisto }.toSet()
        YliopistoEnum.entries.forEach { university ->
            CaseType.entries.forEach { caseType ->
                if (configuration.onKaytossa(university, caseType) && !configuration.isAsyncEnabled(university, caseType)) {
                    require(university in synchronousUniversities) {
                        "Synchronous archiving is enabled for ${university.name} / ${caseType.value}, " +
                            "but no synchronous SynchronousArkistointiAdapter is registered; enable async archiving or register an adapter"
                    }
                }
            }
        }
    }
}
