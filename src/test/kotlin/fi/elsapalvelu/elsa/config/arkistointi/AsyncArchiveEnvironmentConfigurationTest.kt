package fi.elsapalvelu.elsa.config.arkistointi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArchiveProfile
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
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
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
                    assertThat(provider.isAsyncEnabled(university, caseType))
                        .describedAs("$university / $caseType").isFalse()
                    if (university in dynastyUniversities) {
                        assertThat(provider.onKaytossa(university, caseType))
                            .describedAs("$university / $caseType").isFalse()
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("universities")
    fun `shipped configuration routes supported flows to async adapters`(university: YliopistoEnum) {
        val prefix = environmentPrefixes.getValue(university)
        runner(mapOf("APPLICATION_ARKISTOINTI_${prefix}_KAYTOSSA" to "true")).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            if (university in dynastyUniversities) {
                val settings = provider.getConfiguration(university, CaseType.VALMISTUMINEN)
                assertThat(settings.case.documents?.keys).containsExactlyInAnyOrder("yhteenveto", "liite", "laillistamistodistus")
                assertThat(provider.getConfiguration(university, CaseType.KOEJAKSO).case.documents?.keys)
                    .containsExactly("arviointi")
            }
            flows.getValue(university).forEach { caseType ->
                assertThat(provider.isAsyncEnabled(university, caseType))
                    .describedAs("$university / $caseType")
                    .isTrue()
            }
        }
    }

    @ParameterizedTest
    @CsvSource("TURKU,asta-test.utu.fi", "UEF,dynasty-testi.uef.fi")
    fun `ECS deployments inherit Dynasty test URLs when no explicit URLs are supplied`(destination: String, host: String) {
        // The dev, staging and production ECS task definitions all select the prod Spring profile.
        runner().withPropertyValues("spring.profiles.active=prod").run { context ->
            assertThat(context).hasNotFailed()
            val archive = context.getBean(ApplicationProperties::class.java).getArkistointi()
            val dynasty = DynastyArchiveProfile.valueOf(destination).archiveProperties(archive).dynasty
            assertThat(dynasty.baseUrl).isEqualTo("https://$host/dynasty.externalinterface.archive.oauth")
            assertThat(dynasty.tokenUrl).isEqualTo("https://$host/dynasty.identityserver/connect/token")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["TURKU", "UEF"])
    fun `explicit ECS environment URLs override the Dynasty test defaults`(destination: String) {
        val baseUrl = "https://archive.example/api"
        val tokenUrl = "https://identity.example/connect/token"
        runner(mapOf(
            "APPLICATION_ARKISTOINTI_DYNASTY_${destination}_BASE_URL" to baseUrl,
            "APPLICATION_ARKISTOINTI_DYNASTY_${destination}_TOKEN_URL" to tokenUrl
        )).withPropertyValues("spring.profiles.active=prod").run { context ->
            assertThat(context).hasNotFailed()
            val archive = context.getBean(ApplicationProperties::class.java).getArkistointi()
            val dynasty = DynastyArchiveProfile.valueOf(destination).archiveProperties(archive).dynasty
            assertThat(dynasty.baseUrl).isEqualTo(baseUrl)
            assertThat(dynasty.tokenUrl).isEqualTo(tokenUrl)
        }
    }

    @Test
    fun `UEF koejakso defaults to publicity class S`() {
        runner().run { context ->
            assertThat(context).hasNotFailed()
            val archive = context.getBean(ApplicationProperties::class.java).getArkistointi()
            assertThat(archive.getUef().dynasty.koejaksoPublicityClassId).isEqualTo("S")
        }
    }

    @Test
    fun `external tests can override UEF koejakso publicity class`() {
        runner(mapOf(
            "APPLICATION_ARKISTOINTI_DYNASTY_UEF_KOEJAKSO_PUBLICITY_CLASS_ID" to "uef-secret"
        )).withPropertyValues(
            "spring.config.additional-location=classpath:/config/application-external-integration.yml"
        ).run { context ->
            assertThat(context).hasNotFailed()
            val archive = context.getBean(ApplicationProperties::class.java).getArkistointi()
            assertThat(archive.getUef().dynasty.koejaksoPublicityClassId).isEqualTo("uef-secret")
        }
    }

    @Test
    fun `shipped Turku configuration passes worker validation when enabled with credentials`() {
        runner(mapOf(
            "APPLICATION_ARKISTOINTI_TURKU_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_DYNASTY_TURKU_CLIENT_ID" to "client",
            "APPLICATION_ARKISTOINTI_DYNASTY_TURKU_CLIENT_SECRET" to "secret"
        )).withPropertyValues("spring.profiles.active=prod").run { context ->
            assertThat(context).hasNotFailed()
            val properties = context.getBean(ApplicationProperties::class.java)
            val mapper = jacksonObjectMapper()
            val registry = ArkistointiAdapterRegistry(listOf(TurkuDynastyArkistointiAdapter(
                DynastyClient(properties.getArkistointi().getTurku().dynasty, mapper, Clock.systemDefaultZone()),
                TurkuDynastyMetadataMapper(properties, mapper, ArchivePayloadReader(mapper), Clock.systemUTC()),
                DynastyStepExecutor(mock()), properties
            )))

            assertThat(properties.getArkistointi().getTurku().dynasty.tutkintoasetukset.keys)
                .containsExactlyInAnyOrder("678/1998", "1343/2002", "401/2007", "56/2015", "55/2020")
            assertThatCode { registry.validateWorkerConfiguration(context.getBean(ArkistointiConfigurationProvider::class.java)) }
                .doesNotThrowAnyException()
        }
    }

    @ParameterizedTest
    @MethodSource("switchableUniversityFlows")
    fun `ECS environment variable overrides only the selected university and flow`(
        university: YliopistoEnum, caseType: CaseType
    ) {
        val prefix = environmentPrefixes.getValue(university)
        runner(mapOf(
            // Enable all universities so isolation is not hidden by kaytossa=false.
            "APPLICATION_ARKISTOINTI_TRE_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_TURKU_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_UEF_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_HKI_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_${prefix}_METADATA_CASES_${caseType.name}_ASYNCENABLED" to "false"
        )).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            flows.forEach { (otherUniversity, caseTypes) ->
                caseTypes.forEach { otherCase ->
                    assertThat(provider.isAsyncEnabled(otherUniversity, otherCase))
                        .describedAs("$otherUniversity / $otherCase")
                        .isEqualTo(otherUniversity != university || otherCase != caseType)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["TURKU", "UEF"])
    fun `KAYTOSSA enables both Dynasty flows despite legacy async environment overrides`(prefix: String) {
        val university = environmentPrefixes.entries.single { it.value == prefix }.key
        runner(mapOf(
            "APPLICATION_ARKISTOINTI_${prefix}_KAYTOSSA" to "true",
            "APPLICATION_ARKISTOINTI_${prefix}_METADATA_CASES_KOEJAKSO_ASYNCENABLED" to "false",
            "APPLICATION_ARKISTOINTI_${prefix}_METADATA_CASES_VALMISTUMINEN_ASYNCENABLED" to "false"
        )).run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            flows.filterKeys { it in dynastyUniversities }.forEach { (otherUniversity, caseTypes) ->
                caseTypes.forEach { caseType ->
                    assertThat(provider.isAsyncEnabled(otherUniversity, caseType))
                        .describedAs("$otherUniversity / $caseType")
                        .isEqualTo(otherUniversity == university)
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("universityFlows")
    fun `shared test configuration supports opt in to asynchronous routing`(
        university: YliopistoEnum, caseType: CaseType
    ) {
        val testRunner = runner().withPropertyValues(
            "spring.config.location=file:src/main/resources/config/,classpath:/config/application-archive-test.yml"
        )
        val key = environmentPrefixes.getValue(university).lowercase()
        val enabledRunner = testRunner.withPropertyValues("application.arkistointi.$key.kaytossa=true")
        enabledRunner.run { context ->
            assertThat(context).hasNotFailed()
            val provider = context.getBean(ArkistointiConfigurationProvider::class.java)
            assertThat(provider.onKaytossa(university, caseType)).isTrue()
            assertThat(provider.isAsyncEnabled(university, caseType)).isEqualTo(university in dynastyUniversities)
        }
        if (university in dynastyUniversities) return

        enabledRunner.withPropertyValues(
            "application.arkistointi.$key.metadata.cases.${caseType.value}.async-enabled=true"
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
        private val dynastyUniversities = setOf(YliopistoEnum.TURUN_YLIOPISTO, YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
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
        fun switchableUniversityFlows(): Stream<Arguments> = flowArguments(flows.keys - dynastyUniversities)

        @JvmStatic
        fun universityFlows(): Stream<Arguments> = flowArguments(flows.keys)

        private fun flowArguments(universities: Set<YliopistoEnum>): Stream<Arguments> = universities.flatMap { university ->
            flows.getValue(university).map { Arguments.of(university, it) }
        }.stream()
    }
}
