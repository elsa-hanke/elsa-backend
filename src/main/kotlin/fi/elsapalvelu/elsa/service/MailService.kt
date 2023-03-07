package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.User
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import tech.jhipster.config.JHipsterProperties
import java.nio.charset.StandardCharsets
import java.util.*
import jakarta.mail.MessagingException

enum class MailProperty(val property: String) {
    USER("user"),
    BASE_URL("baseUrl"),
    ID("id"),
    NAME("name"),
    YLIOPISTO("yliopisto"),
    ERIKOISALA("erikoisala"),
    TEXT("text"),
    DATE("date"),
    FEEDBACK("feedback"),
    FEEDBACK_TOPIC("feedbackTopic"),
    FEEDBACK_SENDER("feedbackSender"),
    URL_PATH("urlPath")
}

@Service
class MailService(
    private val jHipsterProperties: JHipsterProperties,
    private val javaMailSender: JavaMailSender,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendEmail(
        to: String,
        cc: List<String>? = listOf(),
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    ) {
        log.debug(
            "Send email[multipart '$isMultipart' and html '$isHtml']" +
                " to: '$to', cc: '${cc?.joinToString { it }}' with subject '$subject' and content=$content"
        )

        val mimeMessage = javaMailSender.createMimeMessage()
        try {
            MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name()).apply {
                setTo(to)
                cc?.filterNot { it.isEmpty() }?.toTypedArray().takeUnless { it.isNullOrEmpty() }
                    ?.let { setCc(it) }
                setFrom(jHipsterProperties.mail.from)
                setSubject(subject)
                setText(content, isHtml)
            }
            javaMailSender.send(mimeMessage)
            log.debug("Sent email to User '$to'")
        } catch (ex: MailException) {
            log.warn("Email could not be sent to user '$to': ${ex.message}")
        } catch (ex: MessagingException) {
            log.warn("Email could not be sent to user '$to': ${ex.message}")
        }
    }

    @Async
    fun sendEmailFromTemplate(
        user: User,
        cc: List<String>? = listOf(),
        templateName: String,
        titleKey: String,
        titleProperties: Array<String> = emptyArray(),
        properties: Map<MailProperty, String>
    ) {
        if (user.email == null) {
            log.debug("Sähköpostiosoitetta ei löydy käyttäjälle '${user.login}'")
            return
        }
        var locale = Locale.forLanguageTag("fi")
        if (user.langKey != null) locale = Locale.forLanguageTag(user.langKey)

        val context = Context(locale).apply {
            setVariable(MailProperty.USER.property, user)
            setVariable(MailProperty.BASE_URL.property, jHipsterProperties.mail.baseUrl)
            properties.forEach {
                setVariable(it.key.property, it.value)
            }
        }

        sendEmailFromTemplate(
            user.email!!,
            cc,
            templateName,
            titleKey,
            titleProperties,
            locale,
            context
        )
    }

    @Async
    fun sendEmailFromTemplate(
        to: String?,
        cc: List<String>? = listOf(),
        templateName: String,
        titleKey: String,
        titleProperties: Array<String> = emptyArray(),
        properties: Map<MailProperty, String>
    ) {
        if (to == null) {
            log.debug("Vastaanottajan sähköpostiosoitetta ei ole määritetty.")
            return
        }

        val locale = Locale.forLanguageTag("fi")
        val context = Context(locale).apply {
            setVariable(MailProperty.BASE_URL.property, jHipsterProperties.mail.baseUrl)
            properties.forEach {
                setVariable(it.key.property, it.value)
            }
        }
        sendEmailFromTemplate(to, cc, templateName, titleKey, titleProperties, locale, context)
    }

    private fun sendEmailFromTemplate(
        to: String,
        cc: List<String>? = listOf(),
        templateName: String,
        titleKey: String,
        titleProperties: Array<String>,
        locale: Locale,
        context: Context
    ) {
        val content = templateEngine.process("mail/${templateName}", context)
        val subject = messageSource.getMessage(titleKey, titleProperties, locale)
        sendEmail(to, cc, subject, content, isMultipart = false, isHtml = true)
    }
}
