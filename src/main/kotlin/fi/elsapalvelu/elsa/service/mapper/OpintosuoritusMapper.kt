package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opintosuoritus
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintosuoritusMapper : EntityMapper<OpintosuoritusDTO, Opintosuoritus> {

    @Mappings(
        Mapping(target = "osakokonaisuudet", ignore = true)
    )
    override fun toEntity(dto: OpintosuoritusDTO): Opintosuoritus

}
