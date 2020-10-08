package fi.oulu.elsa.config

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

    fun getCsrf(): Csrf {
        return csrf
    }

    class Csrf {
        val cookie = Cookie()
    }

    class Cookie {
        var domain: String? = null
    }
}
