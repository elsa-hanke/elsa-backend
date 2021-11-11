package fi.elsapalvelu.elsa

import fi.elsapalvelu.elsa.config.ApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.core.env.Environment
import tech.jhipster.config.DefaultProfileUtil
import tech.jhipster.config.JHipsterConstants
import javax.annotation.PostConstruct

@SpringBootApplication
@EnableConfigurationProperties(LiquibaseProperties::class, ApplicationProperties::class)
class ElsaBackendApp(private val env: Environment) {

    private val log = LoggerFactory.getLogger(javaClass)

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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = runApplication<ElsaBackendApp>(*args) { DefaultProfileUtil.addDefaultProfile(this) }.environment
            logApplicationStartup(env)
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
