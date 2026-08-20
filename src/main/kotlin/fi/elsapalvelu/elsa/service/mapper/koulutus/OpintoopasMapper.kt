package fi.elsapalvelu.elsa.service.mapper.koulutus

import fi.elsapalvelu.elsa.domain.koulutus.Opintoopas
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintoopasDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
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
