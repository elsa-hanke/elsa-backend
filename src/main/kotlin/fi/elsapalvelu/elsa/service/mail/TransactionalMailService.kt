package fi.elsapalvelu.elsa.service.mail

import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class TransactionalMailService(
    private val mailService: MailService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendEmailFromTemplate(
        user: User,
        cc: List<String>? = listOf(),
        templateName: String,
        titleKey: String,
        titleProperties: Array<String> = emptyArray(),
        properties: Map<MailProperty, String>
    ) = afterCommit {
        mailService.sendEmailFromTemplate(user, cc, templateName, titleKey, titleProperties, properties)
    }

    fun sendEmailFromTemplate(
        to: String?,
        cc: List<String>? = listOf(),
        templateName: String,
        titleKey: String,
        titleProperties: Array<String> = emptyArray(),
        properties: Map<MailProperty, String>
    ) = afterCommit {
        mailService.sendEmailFromTemplate(to, cc, templateName, titleKey, titleProperties, properties)
    }

    fun sendFeedbackEmail(
        to: String,
        cc: List<String>? = listOf(),
        templateName: String,
        properties: Map<MailProperty, String>
    ) = afterCommit {
        mailService.sendFeedbackEmail(to, cc, templateName, properties)
    }

    private fun afterCommit(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = action()
            })
        } else {
            log.debug("No active transaction, sending mail immediately")
            action()
        }
    }
}
