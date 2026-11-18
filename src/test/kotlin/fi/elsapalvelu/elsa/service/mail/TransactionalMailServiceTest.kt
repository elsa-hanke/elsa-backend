package fi.elsapalvelu.elsa.service.mail

import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.service.kayttaja.MailProperty
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate

class TransactionalMailServiceTest {
    private val mail: MailService = mock()
    private val service = TransactionalMailService(mail)
    private val user = User().apply { email = "test@example.invalid" }
    private val properties = mapOf(MailProperty.ID to "123")

    @Test
    fun `all notification variants are forwarded only after commit`() {
        TransactionTemplate(TestTransactionManager()).executeWithoutResult {
            enqueueAll()
            verifyNoInteractions(mail)
        }
        verifyAll()
    }

    @Test
    fun `explicit rollback discards all notifications`() {
        TransactionTemplate(TestTransactionManager()).executeWithoutResult {
            enqueueAll()
            it.setRollbackOnly()
        }
        verifyNoInteractions(mail)
    }

    @Test
    fun `exception rollback discards all notifications`() {
        assertThrows<IllegalStateException> {
            TransactionTemplate(TestTransactionManager()).executeWithoutResult {
                enqueueAll()
                error("Simulated failure")
            }
        }
        verifyNoInteractions(mail)
    }

    @Test
    fun `commit failure does not send notifications`() {
        assertThrows<TransactionSystemException> {
            TransactionTemplate(TestTransactionManager(failCommit = true)).executeWithoutResult { enqueueAll() }
        }
        verifyNoInteractions(mail)
    }

    @Test
    fun `a rolled back notification does not leak into the next transaction`() {
        val template = TransactionTemplate(TestTransactionManager())
        template.executeWithoutResult {
            enqueueAll()
            it.setRollbackOnly()
        }
        template.executeWithoutResult { verifyNoInteractions(mail) }
        verifyNoInteractions(mail)
    }

    @Test
    fun `without a transaction existing immediate behavior is preserved`() {
        enqueueAll()
        verifyAll()
    }

    private fun enqueueAll() {
        service.sendEmailFromTemplate(user, templateName = "user.html", titleKey = "user.title", properties = properties)
        service.sendEmailFromTemplate("to@example.invalid", templateName = "to.html", titleKey = "to.title", properties = properties)
        service.sendFeedbackEmail("feedback@example.invalid", templateName = "feedback.html", properties = properties)
    }

    private fun verifyAll() {
        verify(mail).sendEmailFromTemplate(user, emptyList(), "user.html", "user.title", emptyArray(), properties)
        verify(mail).sendEmailFromTemplate("to@example.invalid", emptyList(), "to.html", "to.title", emptyArray(), properties)
        verify(mail).sendFeedbackEmail("feedback@example.invalid", emptyList(), "feedback.html", properties)
        verifyNoMoreInteractions(mail)
    }

    /** Exercises Spring's synchronization lifecycle without a database. PostgreSQL coverage lives in IT. */
    private class TestTransactionManager(private val failCommit: Boolean = false) : AbstractPlatformTransactionManager() {
        override fun doGetTransaction() = Any()
        override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit
        override fun doRollback(status: DefaultTransactionStatus) = Unit
        override fun doCommit(status: DefaultTransactionStatus) {
            if (failCommit) throw TransactionSystemException("Simulated commit failure")
        }
    }
}
