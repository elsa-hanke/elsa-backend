package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Disposable Cypress stack only. The real processor and document loader still run. */
@Configuration(proxyBeanMethods = false)
@Profile("e2e-archive & !prod & !replica")
class E2eArchiveConfiguration {
    @Bean
    fun helsinkiE2eAdapter(): ArkistointiAdapter = adapter(YliopistoEnum.HELSINGIN_YLIOPISTO)

    @Bean
    fun tampereE2eAdapter(): ArkistointiAdapter = adapter(YliopistoEnum.TAMPEREEN_YLIOPISTO)

    @Bean
    fun uefE2eAdapter(): ArkistointiAdapter = adapter(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)

    private fun adapter(university: YliopistoEnum): ArkistointiAdapter = object : ArkistointiAdapter {
        override val university = university
        override val supportedCaseTypes = CaseType.entries.toSet()

        override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
            check(pyynto.university == university && pyynto.asiakirjat.isNotEmpty())
            return ArkistointiResult.Success("e2e:$university:${pyynto.jobId}")
        }
    }
}
