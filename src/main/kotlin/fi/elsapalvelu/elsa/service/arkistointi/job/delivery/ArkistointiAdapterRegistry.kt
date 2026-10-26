package fi.elsapalvelu.elsa.service.arkistointi.job.delivery

import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.stereotype.Component

@Component
class ArkistointiAdapterRegistry(adapters: List<ArkistointiAdapter>) {
    private val adaptersByUniversity = adapters.associateBy { it.university }.also {
        require(it.size == adapters.size) { "Multiple archive job adapters configured for the same university" }
    }

    fun find(university: YliopistoEnum): ArkistointiAdapter? = adaptersByUniversity[university]

    fun validateWorkerConfiguration(configuration: ArkistointiConfigurationProvider) {
        YliopistoEnum.entries.forEach { university ->
            CaseType.entries.forEach caseTypes@ { caseType ->
                if (!configuration.isAsyncEnabled(university, caseType)) return@caseTypes
                val adapter = requireNotNull(find(university)) {
                    "Async archiving is enabled for ${university.name} / ${caseType.value}, " +
                        "but no asynchronous ArkistointiAdapter is registered"
                }
                require(caseType in adapter.supportedCaseTypes) {
                    "Async archiving is enabled for ${university.name} / ${caseType.value}, " +
                        "but the asynchronous ArkistointiAdapter does not support this case type"
                }
            }
        }
    }
}
