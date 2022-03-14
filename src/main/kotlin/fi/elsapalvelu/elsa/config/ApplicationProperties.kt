package fi.elsapalvelu.elsa.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Properties specific to Elsa Backend.
 *
 * Properties are configured in the `application.yml` file.
 * See [tech.jhipster.config.JHipsterProperties] for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
class ApplicationProperties {
    private val csrf = Csrf()
    private val security = Security()
    private val feedback = Feedback()

    fun getCsrf(): Csrf {
        return csrf
    }

    fun getSecurity(): Security {
        return security
    }

    fun getFeedback(): Feedback {
        return feedback
    }

    class Csrf {
        val cookie = Cookie()
    }

    class Cookie {
        var domain: String? = null
    }

    class Security {
        var encodedKey: String? = null
        var secretKeyAlgorithm: String? = null
        var cipherAlgorithm: String? = null
        var samlScheme: String? = null

        private val suomifi = Suomifi()
        private val haka = Haka()
        private val sisuHy = SisuHy()

        fun getSuomifi(): Suomifi {
            return suomifi
        }

        fun getHaka(): Haka {
            return haka
        }

        fun getSisuHy(): SisuHy {
            return sisuHy
        }

        class Suomifi {
            var enabled: Boolean? = null
            var samlPrivateKeyLocation: String? = null
            var samlCertificateLocation: String? = null
            var samlSuomifiMetadataLocation: String? = null
            var samlSuomifiEntityId: String? = null
            var samlSuomifiSloUrl: String? = null
        }

        class Haka {
            var enabled: Boolean? = null
            var samlPrivateKeyLocation: String? = null
            var samlCertificateLocation: String? = null
            var samlHakaMetadataLocation: String? = null
        }

        class SisuHy {
            var apiKey: String? = null
            var graphQlEndpointUrl: String? = null
            var tutkintoohjelmaExportUrl: String? = null
            var privateKeyLocation: String? = null
            var certificateLocation: String? = null
        }
    }

    class Feedback {
        var to: String? = null
    }
}
