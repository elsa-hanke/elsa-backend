package fi.elsapalvelu.elsa.config.arkistointi

import fi.elsapalvelu.elsa.config.ApplicationProperties.Arkistointi.Metadata

interface DynastyArchiveProperties {
    val dynasty: DynastyProperties
    var metadata: Metadata?
}
