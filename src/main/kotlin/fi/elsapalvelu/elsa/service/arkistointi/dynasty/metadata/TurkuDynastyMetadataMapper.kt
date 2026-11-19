package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import org.springframework.stereotype.Component

@Component
class TurkuDynastyMetadataMapper(
    properties: ApplicationProperties, mapper: ObjectMapper, reader: ArchivePayloadReader
) : DynastyMetadataMapper(
    YliopistoEnum.TURUN_YLIOPISTO,
    properties.getArkistointi().getTurku().dynasty,
    { properties.getArkistointi().getTurku().metadata },
    mapper,
    reader
)
