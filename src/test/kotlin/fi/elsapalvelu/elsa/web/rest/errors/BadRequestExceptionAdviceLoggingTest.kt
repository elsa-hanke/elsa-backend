package fi.elsapalvelu.elsa.web.rest.errors

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrorCategory
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.louhi.LouhiTransferException
import fi.elsapalvelu.elsa.service.arkistointi.louhi.LouhiTransferStage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.slf4j.LoggerFactory
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.net.SocketTimeoutException

class BadRequestExceptionAdviceLoggingTest {
    @ParameterizedTest
    @CsvSource(
        "louhi,500,ERROR,reason=TIMEOUT",
        "input,400,WARN,ARCHIVE_DOCUMENT_INVALID",
        "stored,500,ERROR,ARCHIVE_DOCUMENT_INVALID",
        "pdf,400,WARN,dataillegal.valmistumispyynnon-liite-ei-ole-kelvollinen-pdf",
        "characters,400,WARN,dataillegal.pdf-tiedostossa-tukemattomia-merkkeja"
    )
    fun `HTTP boundary logs one safe event and preserves document status classification`(
        failure: String, expectedStatus: Int, expectedLevel: String, diagnostic: String
    ) {
        val mvc = MockMvcBuilders.standaloneSetup(FailingController())
            .setControllerAdvice(BadRequestExceptionAdvice()).build()
        val logger = LoggerFactory.getLogger(BadRequestExceptionAdvice::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start(); logger.addAppender(it) }
        try {
            mvc.perform(get("/test-contract-failures/$failure"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))

            assertThat(appender.list).hasSize(1).allSatisfy {
                assertThat(it.level).isEqualTo(Level.toLevel(expectedLevel))
                assertThat(it.formattedMessage).contains(diagnostic)
                    .doesNotContain("PrivatePerson", "/secret/", "password=secret")
                assertThat(it.throwableProxy).isNull()
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @Test
    fun `unexpected exceptions retain existing error logging with root cause`() {
        val mvc = MockMvcBuilders.standaloneSetup(FailingController())
            .setControllerAdvice(BadRequestExceptionAdvice()).build()
        val logger = LoggerFactory.getLogger(BadRequestExceptionAdvice::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start(); logger.addAppender(it) }
        try {
            mvc.perform(get("/test-contract-failures/unexpected"))
                .andExpect(status().isInternalServerError)

            assertThat(appender.list).hasSize(1).allSatisfy {
                assertThat(it.level).isEqualTo(Level.ERROR)
                assertThat(it.formattedMessage).isEqualTo("Unexpected request failed")
                assertThat(it.throwableProxy.className).isEqualTo(IllegalStateException::class.java.name)
                assertThat(it.throwableProxy.cause.className).isEqualTo(IOException::class.java.name)
                assertThat(it.throwableProxy.cause.message).isEqualTo("Database diagnostic detail")
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @RestController
    class FailingController {
        @GetMapping("/test-contract-failures/{failure}")
        fun fail(@PathVariable failure: String): Unit = throw when (failure) {
            "unexpected" -> IllegalStateException("Unexpected request failed", IOException("Database diagnostic detail"))
            "louhi" -> LouhiTransferException.from(SocketTimeoutException(PRIVATE_MESSAGE), LouhiTransferStage.CONNECT)
                .also { it.addSuppressed(IOException(PRIVATE_MESSAGE)) }
            "input", "stored" -> ArkistointiAsiakirjaException(
                ArkistointiError.create("ARCHIVE_DOCUMENT_INVALID", PRIVATE_MESSAGE),
                if (failure == "input") ArkistointiAsiakirjaErrorCategory.INPUT_DOCUMENT
                else ArkistointiAsiakirjaErrorCategory.STORED_DOCUMENT
            ).also { it.addSuppressed(IOException(PRIVATE_MESSAGE)) }
            "pdf" -> InvalidPdfAttachmentException(
                1L, PRIVATE_MESSAGE, InvalidPdfAttachmentSource.TYOSKENTELYJAKSO, cause = IOException(PRIVATE_MESSAGE)
            )
            else -> UnsupportedPdfCharactersException(PRIVATE_MESSAGE, listOf(PRIVATE_MESSAGE))
        }
    }

    companion object {
        private const val PRIVATE_MESSAGE = "PrivatePerson /secret/document.zip password=secret"
    }
}
