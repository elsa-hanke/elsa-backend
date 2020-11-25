package fi.elsapalvelu.elsa.config

import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KeycloakClientConfiguration(
    private val applicationProperties: ApplicationProperties
) {

    @Bean
    fun keycloak(): Keycloak {
        return KeycloakBuilder.builder()
            .serverUrl(applicationProperties.getKeycloak().admin.serverUrl)
            .realm(applicationProperties.getKeycloak().admin.realm)
            .clientId(applicationProperties.getKeycloak().admin.clientId)
            .clientSecret(applicationProperties.getKeycloak().admin.clientSecret)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .build()
    }
}
