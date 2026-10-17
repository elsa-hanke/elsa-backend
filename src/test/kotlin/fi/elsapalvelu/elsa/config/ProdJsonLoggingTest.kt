package fi.elsapalvelu.elsa.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.ConsoleAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.jhipster.config.JHipsterProperties
import tech.jhipster.config.logging.LoggingUtils

class ProdJsonLoggingTest {
    @ParameterizedTest
    @ValueSource(strings = ["fi.elsapalvelu.elsa.audit.AuditLoggingWrapper", "fi.elsapalvelu.elsa.security.SecurityLoggingWrapper"])
    fun jhipsterJsonContextListenerPreservesStructuredLogFields(loggerName: String) {
        val context = LoggerContext()
        try {
            val properties = JHipsterProperties().logging.apply { isUseJsonFormat = true }
            LoggingUtils.addContextListener(context, """{"app_name":"elsaBackend","app_port":"8080"}""", properties)
            context.start()
            val appender = context.getLogger("ROOT").getAppender("CONSOLE") as ConsoleAppender<*>
            val event = LoggingEvent().apply {
                this.loggerName = loggerName
                level = Level.INFO
                message = "Audit event äö"
                timeStamp = System.currentTimeMillis()
                threadName = "test-worker"
                mdcPropertyMap = mapOf("userId" to "user-123")
                setLoggerContextRemoteView(context.loggerContextRemoteView)
            }
            @Suppress("UNCHECKED_CAST")
            val encoder = appender.encoder as ch.qos.logback.core.encoder.Encoder<LoggingEvent>
            val json = ObjectMapper().readTree(encoder.encode(event))
            assertThat(json.path("message").asText()).isEqualTo("Audit event äö")
            assertThat(json.path("logger_name").asText()).endsWith(loggerName.substringAfterLast('.'))
            assertThat(json.path("userId").asText()).isEqualTo("user-123")
            assertThat(json.path("app_name").asText()).isEqualTo("elsaBackend")
            assertThat(json.path("app_port").asText()).isEqualTo("8080")
        } finally {
            context.stop()
        }
    }
}
