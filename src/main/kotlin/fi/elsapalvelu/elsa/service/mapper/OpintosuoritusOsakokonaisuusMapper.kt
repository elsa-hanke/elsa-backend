package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.OpintosuoritusOsakokonaisuus
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusOsakokonaisuusDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintosuoritusOsakokonaisuusMapper :
    EntityMapper<OpintosuoritusOsakokonaisuusDTO, OpintosuoritusOsakokonaisuus>
