package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opintoopas
import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoisalaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintoopasMapper : EntityMapper<OpintoopasDTO, Opintoopas> {

    @Mappings(
        Mapping(source = "arviointiasteikko.id", target = "arviointiasteikkoId"),
        Mapping(source = "arviointiasteikko.nimi", target = "arviointiasteikkoNimi")
    )
    override fun toDto(entity: Opintoopas): OpintoopasDTO

    @Mappings(
        Mapping(source = "arviointiasteikkoId", target = "arviointiasteikko.id")
    )
    override fun toEntity(dto: OpintoopasDTO): Opintoopas
}
