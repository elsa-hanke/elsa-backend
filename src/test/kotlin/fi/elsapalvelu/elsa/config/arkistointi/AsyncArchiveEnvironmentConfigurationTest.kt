package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import java.util.stream.Stream

class AsyncArchiveEnvironmentConfigurationTest {
    @ParameterizedTest
    @MethodSource("universities")
    fun `shipped configuration keeps all supported async flows disabled`(university: YliopistoEnum) {
        runner().run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            if (university == YliopistoEnum.TURUN_YLIOPISTO || university == YliopistoEnum.ITA_SUOMEN_YLIOPISTO) {
                val settings = provider.getConfiguration(university, CaseType.VALMISTUMINEN)
                assertThat(settings.case.documents?.keys).containsExactlyInAnyOrder("yhteenveto", "liite", "laillistamistodistus")
                assertThat(provider.getConfiguration(university, CaseType.KOEJAKSO).case.documents?.keys)
                    .containsExactly("arviointi")
            }
            flows.getValue(university).forEach { caseType ->
                assertThat(provider.getConfiguration(university, caseType).case.asyncEnabled)
                    .describedAs("$university / $caseType").isFalse()
            }
        }
    }

    @ParameterizedTest
    @MethodSource("universityFlows")
    fun `ECS environment variable enables only the selected university and flow`(
        university: YliopistoEnum, caseType: CaseType
    ) {
        val prefix = environmentPrefixes.getValue(university)
        runner(mapOf(
            // Enable both universities so university isolation is not hidden by kaytossa=false.
            "APPLICATION_ARKISTOINTI_TRE_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_TURKU_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_UEF_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_HKI_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_${prefix}_METADATA_CASES_${caseType.name}_ASYNCENABLED" to "true"
        )).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            flows.forEach { (otherUniversity, caseTypes) ->
                caseTypes.forEach { otherCase ->
                    assertThat(provider.isAsyncEnabled(otherUniversity, otherCase))
                        .describedAs("$otherUniversity / $otherCase")
                        .isEqualTo(otherUniversity == university && otherCase == caseType)
                }
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
            "spring.profiles.active=archive-environment-test"
        )
        .withUserConfiguration(BindingConfiguration::class.java, ArkistointiConfigurationProvider::class.java)

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties::class)
    class BindingConfiguration

    companion object {
        private val flows = mapOf(
            YliopistoEnum.TURUN_YLIOPISTO to listOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO),
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO to listOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO),
            YliopistoEnum.TAMPEREEN_YLIOPISTO to listOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO),
            YliopistoEnum.HELSINGIN_YLIOPISTO to listOf(CaseType.VALMISTUMINEN, CaseType.KOEJAKSO, CaseType.SOPIMUS)
        )
        private val environmentPrefixes = mapOf(
            YliopistoEnum.TURUN_YLIOPISTO to "TURKU", YliopistoEnum.ITA_SUOMEN_YLIOPISTO to "UEF",
            YliopistoEnum.TAMPEREEN_YLIOPISTO to "TRE", YliopistoEnum.HELSINGIN_YLIOPISTO to "HKI"
        )

        @JvmStatic
        fun universities(): Stream<Arguments> = flows.keys.stream().map { Arguments.of(it) }

        @JvmStatic
        fun universityFlows(): Stream<Arguments> = flows.flatMap { (university, caseTypes) ->
            caseTypes.map { Arguments.of(university, it) }
        }.stream()
    }
}
