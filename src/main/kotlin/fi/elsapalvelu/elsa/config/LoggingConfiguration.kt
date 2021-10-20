package fi.elsapalvelu.elsa.config

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.more.appenders.DataFluentAppender
import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.audit.AuditLoggingWrapper
import fi.elsapalvelu.elsa.security.SecurityLoggingWrapper
import io.github.jhipster.config.JHipsterProperties
import io.github.jhipster.config.logging.LoggingUtils.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

private const val CONSOLE_APPENDER_NAME = "CONSOLE"

/*
 * Configures the console and Logstash log appenders from the app properties.
 */
@Configuration
class LoggingConfiguration(
    @Value("\${spring.application.name}") appName: String,
    @Value("\${server.port}") serverPort: String,
    jHipsterProperties: JHipsterProperties,
    mapper: ObjectMapper
) {

    init {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext

        val map = mutableMapOf<String, String?>()
        map["app_name"] = appName
        map["app_port"] = serverPort
        val customFields = mapper.writeValueAsString(map)

        val loggingProperties = jHipsterProperties.logging
        val logstashProperties = loggingProperties.logstash

        if (loggingProperties.isUseJsonFormat) {
            val fluentAppender = createFluentAppender("fluent_backend", "backend", context)
            context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(fluentAppender)

            val fluentSecurityAppender =
                createFluentAppender("fluent_security", "security", context)
            context.getLogger(SecurityLoggingWrapper::class.java).addAppender(fluentSecurityAppender)

            val fluentAuditAppender =
                createFluentAppender("fluent_audit", "audit", context)
            context.getLogger(AuditLoggingWrapper::class.java).addAppender(fluentAuditAppender)

            context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(CONSOLE_APPENDER_NAME)
        }
        if (logstashProperties.isEnabled) {
            addLogstashTcpSocketAppender(context, customFields, logstashProperties)
        }
        if (loggingProperties.isUseJsonFormat || logstashProperties.isEnabled) {
            addContextListener(context, customFields, loggingProperties)
        }
        if (jHipsterProperties.metrics.logs.isEnabled) {
            setMetricsMarkerLogbackFilter(context, loggingProperties.isUseJsonFormat)
        }
    }

    private fun createFluentAppender(
        name: String,
        tag: String,
        context: LoggerContext
    ): DataFluentAppender<ILoggingEvent> {
        val appender = DataFluentAppender<ILoggingEvent>()
        appender.context = context
        appender.remoteHost = System.getenv("FLUENT_HOST") // Injected by AWS firelens
        appender.port = System.getenv("FLUENT_PORT").toInt()
        appender.tag = tag
        appender.name = name
        appender.start()
        return appender
    }
}
