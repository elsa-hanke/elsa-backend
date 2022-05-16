package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.service.dto.VastuuhenkilonTehtavatyyppiDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface VastuuhenkilonTehtavatyyppiMapper : EntityMapper<VastuuhenkilonTehtavatyyppiDTO, VastuuhenkilonTehtavatyyppi>
