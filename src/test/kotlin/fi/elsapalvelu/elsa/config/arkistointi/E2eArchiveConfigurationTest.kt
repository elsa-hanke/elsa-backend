package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import fi.elsapalvelu.elsa.service.arkistointi.louhi.LouhiAsyncArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.louhi.TampereLouhiService
import fi.elsapalvelu.elsa.service.arkistointi.siilo.SiiloAsyncArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.siilo.HelsinkiSiiloService
import fi.elsapalvelu.elsa.service.arkistointi.sahke.AsyncSahkePackageBuilder
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.UUID

class E2eArchiveConfigurationTest {
    private val context = ApplicationContextRunner()
        .withBean(AsyncSahkePackageBuilder::class.java, { mock() })
        .withBean(TampereLouhiService::class.java, { mock() })
        .withBean(HelsinkiSiiloService::class.java, { mock() })
        .withUserConfiguration(E2eArchiveConfiguration::class.java,
            LouhiAsyncArkistointiAdapter::class.java, SiiloAsyncArkistointiAdapter::class.java)

    @ParameterizedTest
    @CsvSource("dev,false", "prod,false", "replica,false", "dev,true", "prod,true", "replica,true")
    fun `without the test profile only real adapters are registered`(profiles: String, enabled: Boolean) {
        context.withPropertyValues("spring.profiles.active=$profiles", "application.e2e.archive-stub-enabled=$enabled").run {
            assertThat(it).hasNotFailed()
            assertThat(it.getBeansOfType(ArkistointiAdapter::class.java).values.map { adapter -> adapter.javaClass.name })
                .containsExactlyInAnyOrder(LouhiAsyncArkistointiAdapter::class.java.name, SiiloAsyncArkistointiAdapter::class.java.name)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing", "false", "invalid"])
    fun `test profile requires explicit consent`(flag: String) {
        val runner = context.withPropertyValues("spring.profiles.active=dev,e2e-archive")
        val configured = if (flag == "missing") runner else runner.withPropertyValues("application.e2e.archive-stub-enabled=$flag")
        configured.run {
            assertThat(it).hasFailed()
            assertThat(it.startupFailure).hasRootCauseMessage(
                "The e2e-archive profile requires application.e2e.archive-stub-enabled=true in the disposable test stack"
            )
        }
    }

    @ParameterizedTest
    @CsvSource("prod,true", "replica,true", "prod,false", "replica,false")
    fun `protected profiles reject the test profile regardless of consent`(profile: String, enabled: Boolean) {
        context.withPropertyValues("spring.profiles.active=$profile,e2e-archive", "application.e2e.archive-stub-enabled=$enabled").run {
            assertThat(it).hasFailed()
            assertThat(it.startupFailure).hasRootCauseMessage("The e2e-archive profile cannot be used with prod or replica")
        }
    }

    @Test
    fun `disposable profile provides distinct adapters and a delivery receipt`() {
        context.withPropertyValues("spring.profiles.active=dev,e2e-archive", "application.e2e.archive-stub-enabled=true").run {
            assertThat(it).hasNotFailed()
            val adapters = it.getBeansOfType(ArkistointiAdapter::class.java).values
            assertThat(adapters).hasSize(3)
            assertThat(adapters.none { adapter -> adapter is LouhiAsyncArkistointiAdapter || adapter is SiiloAsyncArkistointiAdapter }).isTrue()
            assertThat(adapters.map { adapter -> adapter.university }).containsExactlyInAnyOrder(
                YliopistoEnum.HELSINGIN_YLIOPISTO, YliopistoEnum.TAMPEREEN_YLIOPISTO, YliopistoEnum.ITA_SUOMEN_YLIOPISTO
            )
            adapters.forEach { adapter ->
                val id = UUID.randomUUID()
                val result = adapter.arkistoi(ArkistointiSubmission(id, adapter.university, CaseType.KOEJAKSO, "{}", listOf(mock())))
                assertThat(result).isEqualTo(ArkistointiResult.Success("e2e:${adapter.university}:$id"))
            }
        }
    }
}
