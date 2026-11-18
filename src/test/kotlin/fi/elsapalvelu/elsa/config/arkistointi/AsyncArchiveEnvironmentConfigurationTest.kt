package fi.elsapalvelu.elsa.config.arkistointi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepExecutor
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.TurkuDynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.mockito.kotlin.mock
import java.time.Clock
import org.junit.jupiter.api.Test
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
    @Test
    fun `default test configuration imports shared archive defaults`() {
        runner().withPropertyValues(
            "spring.config.location=classpath:/config/",
            "spring.profiles.active=test"
        ).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            flows.forEach { (university, caseTypes) ->
                caseTypes.forEach { caseType ->
                    assertThat(provider.getConfiguration(university, caseType).case.asyncEnabled)
                        .describedAs("$university / $caseType").isFalse()
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("universities")
    fun `shipped configuration enables supported async flows except UEF`(university: YliopistoEnum) {
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
                    .describedAs("$university / $caseType")
                    .isEqualTo(university != YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
            }
        }
    }

    @Test
    fun `shipped Turku configuration passes worker validation when enabled with credentials`() {
        runner(mapOf(
            "APPLICATION_ARKISTOINTI_TURKU_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_DYNASTY_TURKU_CLIENT_ID" to "client",
            "APPLICATION_ARKISTOINTI_DYNASTY_TURKU_CLIENT_SECRET" to "secret"
        )).run { context ->
            assertThat(context).hasNotFailed()
            val properties = context.getBean(ApplicationProperties::class.java)
            val mapper = jacksonObjectMapper()
            val registry = ArkistointiAdapterRegistry(listOf(TurkuDynastyArkistointiAdapter(
                DynastyClient(properties.getArkistointi().getTurku().dynasty, mapper, Clock.systemDefaultZone()),
                TurkuDynastyMetadataMapper(properties, mapper, ArchivePayloadReader(mapper)),
                DynastyStepExecutor(mock(), properties)
            )))

            assertThat(properties.getArkistointi().getTurku().dynasty.tutkintoasetukset.keys)
                .containsExactlyInAnyOrder("678/1998", "1343/2002", "401/2007", "56/2015", "55/2020")
            assertThatCode { registry.validateWorkerConfiguration(context.getBean(ArkistointiConfigurationProvider::class.java)) }
                .doesNotThrowAnyException()
        }
    }

    @ParameterizedTest
    @MethodSource("universityFlows")
    fun `ECS environment variable overrides only the selected university and flow`(
        university: YliopistoEnum, caseType: CaseType
    ) {
        val prefix = environmentPrefixes.getValue(university)
        val enabled = university == YliopistoEnum.ITA_SUOMEN_YLIOPISTO
        runner(mapOf(
            // Enable all universities so isolation is not hidden by kaytossa=false.
            "APPLICATION_ARKISTOINTI_TRE_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_TURKU_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_UEF_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_HKI_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_${prefix}_METADATA_CASES_${caseType.name}_ASYNCENABLED" to enabled.toString()
        )).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            flows.forEach { (otherUniversity, caseTypes) ->
                caseTypes.forEach { otherCase ->
                    assertThat(provider.isAsyncEnabled(otherUniversity, otherCase))
                        .describedAs("$otherUniversity / $otherCase")
                        .isEqualTo(if (otherUniversity == university && otherCase == caseType) {
                            enabled
                        } else {
                            otherUniversity != YliopistoEnum.ITA_SUOMEN_YLIOPISTO
                        })
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("universityFlows")
    fun `shared test configuration disables async and allows explicit opt in`(
        university: YliopistoEnum, caseType: CaseType
    ) {
        val testRunner = runner().withPropertyValues(
            "spring.config.location=file:src/main/resources/config/,classpath:/config/application-archive-test.yml"
        )
        testRunner.run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            flows.forEach { (otherUniversity, caseTypes) ->
                caseTypes.forEach { otherCase ->
                    assertThat(provider.getConfiguration(otherUniversity, otherCase).case.asyncEnabled)
                        .describedAs("$otherUniversity / $otherCase").isFalse()
                }
            }
        }
        val key = environmentPrefixes.getValue(university).lowercase()
        testRunner.withPropertyValues(
            "application.arkistointi.$key.kaytossa=true",
            "application.arkistointi.$key.metadata.cases.${caseType.name.lowercase()}.async-enabled=true"
        ).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            assertThat(provider.isAsyncEnabled(university, caseType)).isTrue()
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
