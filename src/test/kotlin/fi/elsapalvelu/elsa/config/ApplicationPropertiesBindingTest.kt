package fi.elsapalvelu.elsa.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource

class ApplicationPropertiesBindingTest {
    @Test
    fun productionSamlSchemeEnvironmentVariableOverridesApplicationYaml() {
        val environment = StandardEnvironment()
        YamlPropertySourceLoader().load("application.yml", FileSystemResource("src/main/resources/config/application.yml"))
            .forEach(environment.propertySources::addLast)
        environment.propertySources.addFirst(SystemEnvironmentPropertySource(
            "test",
            mapOf("APPLICATION_SECURITY_SAML_SCHEME" to "https")
        ))
        val properties = ApplicationProperties()

        Binder.get(environment).bind("application", Bindable.ofInstance(properties))

        assertThat(properties.getSecurity().samlScheme).isEqualTo("https")
    }
}
