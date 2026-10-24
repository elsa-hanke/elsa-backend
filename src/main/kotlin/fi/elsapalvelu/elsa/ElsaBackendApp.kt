package fi.elsapalvelu.elsa

import fi.elsapalvelu.elsa.service.integration.sisu.SisuTutkintoohjelmaImportService
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.StartupLogDetails
import fi.elsapalvelu.elsa.repository.koulutus.ErikoisalaSisuTutkintoohjelmaRepository
import fi.elsapalvelu.elsa.service.integration.sisu.SisuTutkintoohjelmaFetchingService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import tech.jhipster.config.DefaultProfileUtil
import tech.jhipster.config.JHipsterConstants
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_DEVELOPMENT
import java.time.Clock
import java.time.ZoneId

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class ElsaBackendApp(
    private val env: Environment,
    erikoisalaSisuTutkintoohjelmaRepository: ErikoisalaSisuTutkintoohjelmaRepository,
    sisuTutkintoohjelmaFetchingService: SisuTutkintoohjelmaFetchingService,
    sisuTutkintoohjelmaImportService: SisuTutkintoohjelmaImportService,
    applicationProperties: ApplicationProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        Companion.erikoisalaSisuTutkintoohjelmaRepository = erikoisalaSisuTutkintoohjelmaRepository
        Companion.sisuTutkintoohjelmaFetchingService = sisuTutkintoohjelmaFetchingService
        Companion.sisuTutkintoohjelmaImportService = sisuTutkintoohjelmaImportService
        Companion.applicationProperties = applicationProperties
    }

    @PostConstruct
    fun initApplication() {
        val activeProfiles = env.activeProfiles
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
        ) {
            log.error(
                "You have misconfigured your application!" +
                    "It should not run with both the 'dev' and 'prod' profiles at the same time."
            )
        }
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_CLOUD)
        ) {
            log.error(
                "You have misconfigured your application!" +
                    "It should not run with both the 'dev' and 'cloud' profiles at the same time."
            )
        }
    }

    @Bean
    fun clock(): Clock {
        return Clock.systemDefaultZone();
    }

    companion object {

        private lateinit var erikoisalaSisuTutkintoohjelmaRepository: ErikoisalaSisuTutkintoohjelmaRepository
        private lateinit var sisuTutkintoohjelmaFetchingService: SisuTutkintoohjelmaFetchingService
        private lateinit var sisuTutkintoohjelmaImportService: SisuTutkintoohjelmaImportService
        private lateinit var applicationProperties: ApplicationProperties

        @JvmStatic
        fun main(args: Array<String>) {
            val context = runApplication<ElsaBackendApp>(*args) { DefaultProfileUtil.addDefaultProfile(this) }
            val env = context.environment
            logApplicationStartup(
                env,
                context.getBeanProvider(BuildProperties::class.java).getIfAvailable(),
                context.getBeanProvider(GitProperties::class.java).getIfAvailable()
            )

            if (
                env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) &&
                applicationProperties.getStudyDataIntegrations().enabled &&
                erikoisalaSisuTutkintoohjelmaRepository.findAll().isEmpty()
            ) {
                runBlocking {
                    sisuTutkintoohjelmaFetchingService.fetch()?.let {
                            sisuTutkintoohjelmaImportService.import(it)
                        }
                }
            }
        }

        @JvmStatic
        private fun logApplicationStartup(env: Environment, build: BuildProperties?, git: GitProperties?) {
            val log = LoggerFactory.getLogger(ElsaBackendApp::class.java)

            val serverPort = env.getProperty("server.port")
            val contextPath = env.getProperty("server.servlet.context-path") ?: "/"
            val runtime = Runtime.getRuntime()
            val rootLevel = (LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as? ch.qos.logback.classic.Logger)?.effectiveLevel
            val applicationLevel = (LoggerFactory.getLogger("fi.elsapalvelu.elsa") as? ch.qos.logback.classic.Logger)?.effectiveLevel

            log.info(
                """

                ----------------------------------------------------------
                Application '${env.getProperty("spring.application.name")}' is running!
                Local:      http://localhost:$serverPort$contextPath
                Profile(s): ${env.activeProfiles.joinToString(",")}
                Build version: ${build?.version ?: "unavailable"}
                Git commit: ${git?.get("commit.id.abbrev") ?: git?.shortCommitId ?: "unavailable"}
                Java: ${Runtime.version()}
                Kotlin: ${KotlinVersion.CURRENT}
                Heap committed: ${runtime.totalMemory() / (1024 * 1024)} MiB
                Heap max: ${runtime.maxMemory() / (1024 * 1024)} MiB
                JVM available processors: ${runtime.availableProcessors()}
                JVM timezone: ${ZoneId.systemDefault()}
                Database: ${StartupLogDetails.databaseAddress(env.getProperty("spring.datasource.url"))}
                Log level (root): ${rootLevel ?: "unavailable"}
                Log level (fi.elsapalvelu.elsa): ${applicationLevel ?: "unavailable"}
                Background scheduling enabled: ${applicationProperties.getScheduling().enabled}
                Study data integrations enabled: ${applicationProperties.getStudyDataIntegrations().enabled}
                ----------------------------------------------------------
                """.trimIndent()
            )
        }
    }
}
