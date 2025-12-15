package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
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
    private val arkistointi = Arkistointi()
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

    fun getArkistointi(): Arkistointi {
        return arkistointi
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

    class Arkistointi {

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
            var kaytossa: Boolean = false
            var metadata: Metadata? = null
        }

        class Hki {
            var kaytossa: Boolean = false
            var host: String? = null
            var apiKey: String? = null
            var metadata: Metadata? = null
        }

        class Tre {
            var kaytossa: Boolean = false
            var host: String? = null
            var port: String? = null
            var user: String? = null
            var privateKeyLocation: String? = null
            var metadata: Metadata? = null
        }

        class Turku {
            var kaytossa: Boolean = false
            var metadata: Metadata? = null
        }

        class Uef {
            var kaytossa: Boolean = false
            var metadata: Metadata? = null
        }

        class Metadata {
            var zipMetadata: Boolean = false
            var contact: Contact? = null
            var organisation: String? = null
            var retentionReason: String? = null
            var retentionPeriod: String? = null
            var useType: String? = null
            var cases: Map<String, Case>? = null

            fun getDocumentMetadata(recordType: RecordType, case: Case?): DocumentMetadata? {
                return case?.documents?.get(recordType.name.lowercase())
            }

            fun getCaseMetadata(caseType: CaseType): Case? {
                return cases?.get(caseType.value)
            }
        }

        class Contact {
            var person: String? = null
            var address: String? = null
            var phone: String? = null
            var email: String? = null
        }

        class Case {
            var title: String? = null
            var type: String? = null
            var siiloKoodi: String? = null
            var function: String? = null
            var documents: Map<String, DocumentMetadata>? = null
        }

        class DocumentMetadata {
            var retentionPeriod: String? = null
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
