package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opintoopas
import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO
import org.mapstruct.Mapper
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ArviointiasteikkoMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintoopasMapper : EntityMapper<OpintoopasDTO, Opintoopas> {

    @Mappings()
    override fun toDto(entity: Opintoopas): OpintoopasDTO

    @Mappings()
    override fun toEntity(dto: OpintoopasDTO): Opintoopas
}
