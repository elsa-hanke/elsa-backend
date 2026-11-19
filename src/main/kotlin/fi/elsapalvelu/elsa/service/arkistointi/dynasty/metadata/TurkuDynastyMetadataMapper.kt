package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyDestination
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class TurkuDynastyMetadataMapper(
    properties: ApplicationProperties, mapper: ObjectMapper, reader: ArchivePayloadReader, clock: Clock
) : DynastyMetadataMapper(
    DynastyDestination.TURKU,
    DynastyDestination.TURKU.archiveProperties(properties.getArkistointi()),
    mapper,
    reader,
    clock
)
