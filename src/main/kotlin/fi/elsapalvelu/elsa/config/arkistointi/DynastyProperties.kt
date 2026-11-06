package fi.elsapalvelu.elsa.config.arkistointi

import java.time.Duration

/** Credentials are deliberately excluded from diagnostic output. */
class DynastyProperties {
    var baseUrl: String? = null
    var tokenUrl: String? = null
    var clientId: String? = null
    var clientSecret: String? = null
    var organization: String? = null
    var scope: String = "external_interface_api"
    var caseSchemaId: String? = null
    var documentSchemaId: String? = null
    var callTimeout: Duration = Duration.ofMinutes(2)
    var inProgressGrace: Duration = Duration.ofMinutes(5)
    var allowLoopbackHttpForTests: Boolean = false
    var tutkintoasetukset: Map<String, String> = emptyMap()
    // Must be confirmed with the vendor before KOEJAKSO can be delivered.
    var koejaksoPublicityClassId: String? = null
    var koejaksoSecrecyFields: Map<String, String> = emptyMap()
    override fun toString() = "DynastyProperties()"
}
