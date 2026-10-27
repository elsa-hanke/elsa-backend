package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource

class TampereAsyncEnvironmentConfigurationTest {
    @Test
    fun `shipped configuration keeps both Tampere async flows disabled`() {
        runner().run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            caseTypes.forEach {
                assertThat(provider.getConfiguration(YliopistoEnum.TAMPEREEN_YLIOPISTO, it).case.asyncEnabled).isFalse()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["VALMISTUMINEN", "KOEJAKSO"])
    fun `ECS environment variable enables only the selected Tampere flow`(caseName: String) {
        runner(mapOf(
            "APPLICATION_ARKISTOINTI_TRE_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_TRE_METADATA_CASES_${caseName}_ASYNCENABLED" to "true"
        )).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            caseTypes.forEach {
                assertThat(provider.isAsyncEnabled(YliopistoEnum.TAMPEREEN_YLIOPISTO, it))
                    .describedAs(it.name).isEqualTo(it.name == caseName)
            }
        }
    }

    private fun runner(variables: Map<String, Any> = emptyMap()) = ApplicationContextRunner()
        .withInitializer(ConfigDataApplicationContextInitializer())
        .withInitializer { context ->
            // Use actual environment-variable binding, not dotted test properties.
            val name = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME
            context.environment.propertySources.replace(name, SystemEnvironmentPropertySource(name, variables))
        }
        .withPropertyValues(
            "spring.config.location=file:src/main/resources/config/",
            "spring.profiles.active=tampere-environment-test"
        )
        .withUserConfiguration(BindingConfiguration::class.java, ArkistointiConfigurationProvider::class.java)

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties::class)
    class BindingConfiguration

    private val caseTypes = listOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO)
}
