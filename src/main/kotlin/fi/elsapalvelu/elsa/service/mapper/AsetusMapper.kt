package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.perustiedot.Asetus
import fi.elsapalvelu.elsa.service.dto.AsetusDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface AsetusMapper : EntityMapper<AsetusDTO, Asetus>
