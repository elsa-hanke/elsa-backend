package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Disposable Cypress stack only. The real processor and document loader still run. */
@Configuration(proxyBeanMethods = false)
@Profile("e2e-archive")
@Lazy(false)
class E2eArchiveConfiguration(environment: Environment) {
    init {
        check(!environment.acceptsProfiles(Profiles.of("prod", "replica"))) {
            "The e2e-archive profile cannot be used with prod or replica"
        }
        check(environment.getProperty("application.e2e.archive-stub-enabled") == "true") {
            "The e2e-archive profile requires application.e2e.archive-stub-enabled=true in the disposable test stack"
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "application.e2e", name = ["archive-stub-enabled"], havingValue = "true")
    fun helsinkiE2eAdapter(): ArkistointiAdapter = adapter(YliopistoEnum.HELSINGIN_YLIOPISTO)

    @Bean
    @ConditionalOnProperty(prefix = "application.e2e", name = ["archive-stub-enabled"], havingValue = "true")
    fun tampereE2eAdapter(): ArkistointiAdapter = adapter(YliopistoEnum.TAMPEREEN_YLIOPISTO)

    @Bean
    @ConditionalOnProperty(prefix = "application.e2e", name = ["archive-stub-enabled"], havingValue = "true")
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
