package fi.elsapalvelu.elsa.security

import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal

/** Test fixture for the legacy principal contract used by login, logout and impersonation. */
@Suppress("DEPRECATION", "Deprecation")
fun testSamlPrincipal(
    name: String?,
    attributes: Map<String, List<Any?>?>,
    sessionIndexes: List<String> = emptyList()
): DefaultSaml2AuthenticatedPrincipal = DefaultSaml2AuthenticatedPrincipal(name, attributes, sessionIndexes)
