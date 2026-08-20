package fi.elsapalvelu.elsa.service.mapper.perustiedot

import fi.elsapalvelu.elsa.domain.perustiedot.Asetus
import fi.elsapalvelu.elsa.service.dto.perustiedot.AsetusDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface AsetusMapper : EntityMapper<AsetusDTO, Asetus>
