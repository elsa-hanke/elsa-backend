package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDestination
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import org.springframework.stereotype.Component

@Component
class UefDynastyMetadataMapper(
    properties: ApplicationProperties, mapper: ObjectMapper, reader: ArchivePayloadReader
) : DynastyMetadataMapper(
    DynastyDestination.UEF,
    DynastyDestination.UEF.archiveProperties(properties.getArkistointi()),
    mapper,
    reader
)
