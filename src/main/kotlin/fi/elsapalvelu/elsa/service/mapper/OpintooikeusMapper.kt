package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoistuvaLaakariMapper::class,
        YliopistoMapper::class,
        ErikoisalaMapper::class,
        OpintoopasMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintooikeusMapper :
    EntityMapper<OpintooikeusDTO, Opintooikeus> {

    @Mappings(
        Mapping(source = "yliopisto.nimi", target = "yliopistoNimi"),
        Mapping(source = "erikoisala.id", target = "erikoisalaId"),
        Mapping(source = "erikoisala.nimi", target = "erikoisalaNimi"),
        Mapping(source = "opintoopas.id", target = "opintoopasId"),
    )
    override fun toDto(entity: Opintooikeus): OpintooikeusDTO

    @Mappings(
        Mapping(source = "erikoisalaId", target = "erikoisala")
    )
    override fun toEntity(dto: OpintooikeusDTO): Opintooikeus
}
