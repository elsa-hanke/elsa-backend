package fi.elsapalvelu.elsa.service.mapper.perustiedot

import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.service.dto.perustiedot.VastuuhenkilonTehtavatyyppiDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface VastuuhenkilonTehtavatyyppiMapper : EntityMapper<VastuuhenkilonTehtavatyyppiDTO, VastuuhenkilonTehtavatyyppi>
