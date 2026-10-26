package fi.elsapalvelu.elsa.service.arkistointi

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

/** Captures application messages and throwable chains, including generic DEBUG logging. */
internal class ArkistointiLogCapture : AutoCloseable {
    private val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    private val application = LoggerFactory.getLogger("fi.elsapalvelu.elsa") as Logger
    private val previousLevel = application.level
    private val appender = ListAppender<ILoggingEvent>().apply { start() }

    init {
        application.level = Level.DEBUG
        root.addAppender(appender)
    }

    fun assertDoesNotContain(vararg secrets: String) {
        val messages = appender.list.filter { it.loggerName.startsWith("fi.elsapalvelu.elsa") }
            .joinToString("\n") { event ->
                event.formattedMessage + event.throwableProxy?.let(ThrowableProxyUtil::asString).orEmpty()
            }
        assertThat(messages).doesNotContain(*secrets)
    }

    override fun close() {
        root.detachAppender(appender)
        appender.stop()
        application.level = previousLevel
    }
}
