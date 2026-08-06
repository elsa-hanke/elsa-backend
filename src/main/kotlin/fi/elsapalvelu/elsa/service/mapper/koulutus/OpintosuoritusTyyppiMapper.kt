package fi.elsapalvelu.elsa.service.mapper.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.OpintosuoritusTyyppi
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintosuoritusTyyppiDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintosuoritusTyyppiMapper : EntityMapper<OpintosuoritusTyyppiDTO, OpintosuoritusTyyppi>

