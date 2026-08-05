package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.seuranta.Ilmoitus
import fi.elsapalvelu.elsa.service.dto.IlmoitusDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface IlmoitusMapper : EntityMapper<IlmoitusDTO, Ilmoitus>
