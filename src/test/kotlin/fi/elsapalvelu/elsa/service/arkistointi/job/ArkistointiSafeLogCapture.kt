package fi.elsapalvelu.elsa.service.arkistointi.job

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

internal class ArkistointiSafeLogCapture : AutoCloseable {
    private val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    private val appender = ListAppender<ILoggingEvent>().also { it.start(); logger.addAppender(it) }

    fun assertDoesNotContain(secret: String) {
        val events = synchronized(appender) { appender.list.toList() }
        assertThat(events).isNotEmpty
        assertThat(events.map { it.formattedMessage + it.throwableProxy?.let(ThrowableProxyUtil::asString).orEmpty() })
            .allSatisfy { assertThat(it).doesNotContain(secret) }
    }

    override fun close() {
        logger.detachAppender(appender)
        appender.stop()
    }
}
