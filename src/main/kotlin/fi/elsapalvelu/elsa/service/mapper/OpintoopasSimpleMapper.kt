package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opintoopas
import fi.elsapalvelu.elsa.service.dto.OpintoopasSimpleDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintoopasSimpleMapper : EntityMapper<OpintoopasSimpleDTO, Opintoopas> {

    @Mappings(
        Mapping(source = "erikoisala.id", target = "erikoisalaId"),
    )
    override fun toDto(entity: Opintoopas): OpintoopasSimpleDTO

    @Mappings()
    override fun toEntity(dto: OpintoopasSimpleDTO): Opintoopas
}
