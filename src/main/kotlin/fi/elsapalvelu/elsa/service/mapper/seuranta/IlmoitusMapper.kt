package fi.elsapalvelu.elsa.service.mapper.seuranta

import fi.elsapalvelu.elsa.domain.seuranta.Ilmoitus
import fi.elsapalvelu.elsa.service.dto.seuranta.IlmoitusDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface IlmoitusMapper : EntityMapper<IlmoitusDTO, Ilmoitus>
