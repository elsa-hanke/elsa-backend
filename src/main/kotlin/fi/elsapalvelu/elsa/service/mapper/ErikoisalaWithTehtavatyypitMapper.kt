package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.service.dto.ErikoisalaWithTehtavatyypitDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ErikoisalaWithTehtavatyypitMapper : EntityMapper<ErikoisalaWithTehtavatyypitDTO, Erikoisala>

