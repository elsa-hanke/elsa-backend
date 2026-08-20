package fi.elsapalvelu.elsa.service.mapper.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.Opintoopas
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintoopasSimpleDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
