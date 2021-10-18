package fi.elsapalvelu.elsa.security

import org.slf4j.LoggerFactory

class SecurityLoggingWrapper {

    companion object {
        private val log = LoggerFactory.getLogger(SecurityLoggingWrapper::class.java)

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
