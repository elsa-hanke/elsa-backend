package fi.elsapalvelu.elsa.audit

import org.slf4j.LoggerFactory

class AuditLoggingWrapper {

    companion object {
        private val log = LoggerFactory.getLogger(AuditLoggingWrapper::class.java)

        @JvmStatic
        fun info(message: String) {
            log.info(message)
        }

        @JvmStatic
        fun warn (message: String) {
            log.warn(message)
        }
    }
}
