package fi.elsapalvelu.elsa.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.more.appenders.DataFluentAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.msgpack.MessagePack
import java.net.InetAddress
import java.net.ServerSocket

class FluentAppenderTest {
    @ParameterizedTest
    @ValueSource(strings = ["backend", "security", "audit"])
    fun sendsLogEventToFluentSocket(tag: String) {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 5_000
            val context = LoggerContext()
            val appender = DataFluentAppender<ILoggingEvent>().apply {
                this.context = context
                remoteHost = "127.0.0.1"
                port = server.localPort
                this.tag = tag
                start()
            }

            try {
                appender.doAppend(
                    LoggingEvent().apply {
                        loggerName = "fi.elsapalvelu.elsa.audit.AuditLoggingWrapper"
                        level = Level.INFO
                        message = "Audit event äö"
                        threadName = "test-worker"
                        mdcPropertyMap = mapOf("userId" to "user-123")
                    }
                )

                server.accept().use { socket ->
                    socket.soTimeout = 5_000
                    val unpacker = MessagePack().createUnpacker(socket.getInputStream())
                    assertThat(unpacker.readArrayBegin()).isEqualTo(3)
                    assertThat(unpacker.readString()).isEqualTo(tag)
                    assertThat(unpacker.readLong()).isPositive()
                    val fields = (1..unpacker.readMapBegin()).associate {
                        unpacker.readString() to unpacker.readString()
                    }
                    assertThat(fields).containsEntry("message", "Audit event äö")
                    assertThat(fields).containsEntry("logger", "fi.elsapalvelu.elsa.audit.AuditLoggingWrapper")
                    assertThat(fields).containsEntry("level", "INFO")
                    assertThat(fields).containsEntry("userId", "user-123")
                }
            } finally {
                appender.stop()
                context.stop()
            }
        }
    }
}
