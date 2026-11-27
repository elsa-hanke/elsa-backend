package fi.elsapalvelu.elsa.extensions

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

internal fun runAfterCommit(onImmediate: () -> Unit = {}, action: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = action()
        })
    } else {
        onImmediate()
        action()
    }
}
