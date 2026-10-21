package fi.elsapalvelu.elsa.web.rest.errors

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.slf4j.LoggerFactory

class ArkistointiAsiakirjaExceptionAdviceTest {
    @ParameterizedTest
    @CsvSource(
        "INPUT_DOCUMENT,ARCHIVE_DOCUMENT_MISSING,400", "INPUT_DOCUMENT,ARCHIVE_DOCUMENT_EMPTY,400",
        "INPUT_DOCUMENT,ARCHIVE_DOCUMENT_INVALID,400", "STORED_DOCUMENT,ARCHIVE_DOCUMENT_MISSING,500",
        "STORED_DOCUMENT,ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH,500",
        "PROCESSING_FAILURE,ARCHIVE_DOCUMENT_PROCESSING_FAILED,500"
    )
    fun `HTTP status follows category without exposing document details or original exceptions`(
        category: ArkistointiAsiakirjaErrorCategory, code: String, status: Int
    ) {
        val secret = "private-name-and-original-document-contents"
        val exception = ArkistointiAsiakirjaException(ArkistointiError.create(code, secret), category)
        exception.addSuppressed(IllegalStateException(secret, IllegalArgumentException(secret)))
        val logger = LoggerFactory.getLogger(BadRequestExceptionAdvice::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start(); logger.addAppender(it) }
        try {
            val response = BadRequestExceptionAdvice().handleArkistointiAsiakirjaException(exception)

            assertThat(response.status).isEqualTo(status)
            assertThat(response.detail).doesNotContain(secret)
            assertThat(response.properties).containsEntry("message", "error.$code")
                .containsEntry("errorKey", code).doesNotContainKey("params")
            assertThat(response.toString()).doesNotContain(secret)
            assertThat(appender.list).hasSize(1).allSatisfy {
                assertThat(it.level).isEqualTo(if (status == 400) Level.WARN else Level.ERROR)
                assertThat(it.formattedMessage).contains(code).doesNotContain(secret)
                assertThat(it.throwableProxy).isNull()
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
