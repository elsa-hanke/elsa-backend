package fi.elsapalvelu.elsa.service.arkistointi.job

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiConfigurationProvider
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
            CaseType.entries.forEach { caseType ->
                require(!configuration.isAsyncEnabled(university, caseType) || find(university) != null) {
                    "Async archiving is enabled for ${university.name} / ${caseType.value}, " +
                        "but no asynchronous ArkistointiAdapter is registered"
                }
            }
        }
    }
}
