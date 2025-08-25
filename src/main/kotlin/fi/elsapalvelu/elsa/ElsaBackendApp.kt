package fi.elsapalvelu.elsa

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.repository.ErikoisalaSisuTutkintoohjelmaRepository
import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaFetchingService
import fi.elsapalvelu.elsa.service.SisuTutkintoohjelmaImportService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import tech.jhipster.config.DefaultProfileUtil
import tech.jhipster.config.JHipsterConstants
import tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_DEVELOPMENT
import java.time.Clock

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class ElsaBackendApp(
    private val env: Environment,
    erikoisalaSisuTutkintoohjelmaRepository: ErikoisalaSisuTutkintoohjelmaRepository,
    sisuTutkintoohjelmaFetchingService: SisuTutkintoohjelmaFetchingService,
    sisuTutkintoohjelmaImportService: SisuTutkintoohjelmaImportService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        Companion.erikoisalaSisuTutkintoohjelmaRepository = erikoisalaSisuTutkintoohjelmaRepository
        Companion.sisuTutkintoohjelmaFetchingService = sisuTutkintoohjelmaFetchingService
        Companion.sisuTutkintoohjelmaImportService = sisuTutkintoohjelmaImportService
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

        @JvmStatic
        fun main(args: Array<String>) {
            val env = runApplication<ElsaBackendApp>(*args) { DefaultProfileUtil.addDefaultProfile(this) }.environment
            logApplicationStartup(env)

            if (env.activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) && erikoisalaSisuTutkintoohjelmaRepository.findAll()
                    .isEmpty()
            ) {
                runBlocking {
                    sisuTutkintoohjelmaFetchingService.fetch()?.let {
                            sisuTutkintoohjelmaImportService.import(it)
                        }
                }
            }
        }

        @JvmStatic
        private fun logApplicationStartup(env: Environment) {
            val log = LoggerFactory.getLogger(ElsaBackendApp::class.java)

            val serverPort = env.getProperty("server.port")
            val contextPath = env.getProperty("server.servlet.context-path") ?: "/"

            log.info(
                """

                ----------------------------------------------------------
                Application '${env.getProperty("spring.application.name")}' is running!
                Local:      http://localhost:$serverPort$contextPath
                Profile(s): ${env.activeProfiles.joinToString(",")}
                ----------------------------------------------------------
                """.trimIndent()
            )
        }
    }
}
