package fi.elsapalvelu.elsa.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Properties specific to Elsa Backend.
 *
 * Properties are configured in the `application.yml` file.
 * See [io.github.jhipster.config.JHipsterProperties] for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
class ApplicationProperties {
    private val csrf = Csrf()
    private val keycloak = Keycloak()
    private val security = Security()

    fun getCsrf(): Csrf {
        return csrf
    }

    fun getKeycloak(): Keycloak {
        return keycloak
    }

    fun getSecurity(): Security {
        return security
    }

    class Csrf {
        val cookie = Cookie()
    }

    class Cookie {
        var domain: String? = null
    }

    class Keycloak {
        val admin = Admin()
    }

    class Admin {
        var serverUrl: String? = null
        var realm: String? = null
        var clientId: String? = null
        var clientSecret: String? = null
    }

    class Security {
        var encodedKey: String? = null
        var secretKeyAlgorithm: String? = null
        var cipherAlgorithm: String? = null
        var samlPrivateKeyLocation: String? = null
        var samlCertificateLocation: String? = null
        var samlScheme: String? = null
    }
}
