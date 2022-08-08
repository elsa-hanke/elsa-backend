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
    private val sarakesign = Sarakesign()
    private val opintohallintoemail = Opintohallintoemail()

    fun getCsrf(): Csrf {
        return csrf
    }

    fun getSecurity(): Security {
        return security
    }

    fun getFeedback(): Feedback {
        return feedback
    }

    fun getSarakesign(): Sarakesign {
        return sarakesign
    }

    fun getOpintohallintoemail(): Opintohallintoemail {
        return opintohallintoemail
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
        private val peppiOulu = PeppiOulu()
        private val peppiTurku = PeppiTurku()
        private val sisuTre = SisuTre()
        private val peppiUef = PeppiUef()

        fun getSuomifi(): Suomifi {
            return suomifi
        }

        fun getHaka(): Haka {
            return haka
        }

        fun getSisuHy(): SisuHy {
            return sisuHy
        }

        fun getPeppiOulu(): PeppiOulu {
            return peppiOulu
        }

        fun getPeppiTurku(): PeppiTurku {
            return peppiTurku
        }

        fun getSisuTre(): SisuTre {
            return sisuTre
        }

        fun getPeppiUef(): PeppiUef {
            return peppiUef
        }

        class Suomifi {
            var enabled: Boolean? = null
            var samlPrivateKeyLocation: String? = null
            var samlCertificateLocation: String? = null
            var samlSuomifiMetadataLocation: String? = null
            var samlSuomifiEntityId: String? = null
        }

        class Haka {
            var enabled: Boolean? = null
            var samlPrivateKeyLocation: String? = null
            var samlCertificateLocation: String? = null
            var samlHakaMetadataLocation: String? = null
        }

        class SisuHy {
            var apiKey: String? = null
            var graphqlEndpointUrl: String? = null
            var tutkintoohjelmaExportUrl: String? = null
            var privateKeyLocation: String? = null
            var certificateLocation: String? = null
        }

        class PeppiOulu {
            var token: String? = null
            var graphqlEndpointUrl: String? = null
        }

        class PeppiTurku {
            var apiKey: String? = null
            var basicAuthEncodedKey: String? = null
            var endpointUrl: String? = null
        }

        class SisuTre {
            var endpointUrl: String? = null
            var tokenEndpointUrl: String? = null
            var tenantId: String? = null
            var clientId: String? = null
            var scopeId: String? = null
            var clientSecret: String? = null
            var subscriptionKey: String? = null
        }

        class PeppiUef {
            var apiKey: String? = null
            var endpointUrl: String? = null
        }
    }

    class Feedback {
        var to: String? = null
    }

    class Sarakesign {

        private val oulu = Oulu()
        private val hki = Hki()
        private val tre = Tre()
        private val turku = Turku()
        private val uef = Uef()

        fun getOulu(): Oulu {
            return oulu
        }

        fun getHki(): Hki {
            return hki
        }

        fun getTre(): Tre {
            return tre
        }

        fun getTurku(): Turku {
            return turku
        }

        fun getUef(): Uef {
            return uef
        }

        class Oulu {
            var apiKey: String? = null
            var apiUrl: String? = null
            var requestTemplateId: String? = null
        }

        class Hki {
            var apiKey: String? = null
            var apiUrl: String? = null
            var requestTemplateId: String? = null
        }

        class Tre {
            var apiKey: String? = null
            var apiUrl: String? = null
            var requestTemplateId: String? = null
        }

        class Turku {
            var apiKey: String? = null
            var apiUrl: String? = null
            var requestTemplateId: String? = null
        }

        class Uef {
            var apiKey: String? = null
            var apiUrl: String? = null
            var requestTemplateId: String? = null
        }
    }

    class Opintohallintoemail {
        var hki: String? = null
        var oulu: String? = null
        var tre: String? = null
        var turku: String? = null
        var uef: String? = null
    }
 }
