package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.OpintosuoritusTyyppi
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusTyyppiDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintosuoritusTyyppiMapper : EntityMapper<OpintosuoritusTyyppiDTO, OpintosuoritusTyyppi>

