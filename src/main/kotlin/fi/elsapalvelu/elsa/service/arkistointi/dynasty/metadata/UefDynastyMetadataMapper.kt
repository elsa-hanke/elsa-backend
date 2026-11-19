package fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import org.springframework.stereotype.Component

@Component
class UefDynastyMetadataMapper(
    properties: ApplicationProperties, mapper: ObjectMapper, reader: ArchivePayloadReader
) : DynastyMetadataMapper(
    YliopistoEnum.ITA_SUOMEN_YLIOPISTO,
    properties.getArkistointi().getUef().dynasty,
    { properties.getArkistointi().getUef().metadata },
    mapper,
    reader
)
