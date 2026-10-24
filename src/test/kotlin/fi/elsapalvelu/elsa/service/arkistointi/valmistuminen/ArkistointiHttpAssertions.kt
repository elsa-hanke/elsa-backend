package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestExceptionAdvice
import fi.elsapalvelu.elsa.web.rest.vastuuhenkilo.VastuuhenkiloValmistumispyyntoResource
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

internal const val PRIVATE_CERTIFICATE_NAME = "private-approval-certificate"

/** Capture logs only during the request, not fixture setup or database diagnostics. */
internal fun assertArchiveDocumentHttpFailure(
    mvc: MockMvc, requestId: Long, approverId: String, expectedStatus: Int, errorKey: String
): ArkistointiAsiakirjaException {
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    val appender = ListAppender<ILoggingEvent>().also { it.start(); root.addAppender(it) }
    val auth = Saml2Authentication(testSamlPrincipal(approverId, mapOf()), "test",
        listOf(SimpleGrantedAuthority(VASTUUHENKILO)))
    try {
        val response = mvc.perform(put("/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/{id}", requestId)
            .with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().`is`(expectedStatus))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.message").value("error.$errorKey"))
            .andExpect(jsonPath("$.errorKey").value(errorKey))
            .andExpect(jsonPath("$.params").doesNotExist())
            .andReturn()
        assertThat(response.response.contentAsString).doesNotContain(PRIVATE_CERTIFICATE_NAME)
        val events = appender.list.toList()
        assertThat(events.map { it.formattedMessage + it.throwableProxy?.let(ThrowableProxyUtil::asString).orEmpty() })
            .allSatisfy { assertThat(it).doesNotContain(PRIVATE_CERTIFICATE_NAME) }
        val handlers = setOf(BadRequestExceptionAdvice::class.java.name, VastuuhenkiloValmistumispyyntoResource::class.java.name)
        val errorEvents = events.filter { it.loggerName in handlers }
        assertThat(errorEvents).isNotEmpty.allSatisfy { assertThat(it.throwableProxy).isNull() }
        return response.resolvedException as ArkistointiAsiakirjaException
    } finally {
        root.detachAppender(appender)
        appender.stop()
    }
}
