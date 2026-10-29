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
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.UUID

class E2eArchiveConfigurationTest {
    private val context = ApplicationContextRunner().withUserConfiguration(E2eArchiveConfiguration::class.java)

    @ParameterizedTest
    @ValueSource(strings = ["dev", "prod", "replica", "prod,e2e-archive", "replica,e2e-archive"])
    fun `test adapters cannot run in normal or protected profiles`(profiles: String) {
        context.withPropertyValues("spring.profiles.active=$profiles").run {
            assertThat(it.getBeansOfType(ArkistointiAdapter::class.java)).isEmpty()
        }
    }

    @Test
    fun `disposable profile provides distinct adapters and a delivery receipt`() {
        context.withPropertyValues("spring.profiles.active=dev,e2e-archive").run {
            val adapters = it.getBeansOfType(ArkistointiAdapter::class.java).values
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
