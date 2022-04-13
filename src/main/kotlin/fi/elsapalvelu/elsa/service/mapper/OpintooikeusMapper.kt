package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO
import org.mapstruct.*

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoistuvaLaakariMapper::class,
        YliopistoMapper::class,
        ErikoisalaMapper::class,
        OpintoopasMapper::class,
        AsetusMapper::class,
        OpintoopasMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpintooikeusMapper :
    EntityMapper<OpintooikeusDTO, Opintooikeus> {

    @Mappings(
        Mapping(source = "yliopisto.id", target = "yliopistoId"),
        Mapping(source = "yliopisto.nimi", target = "yliopistoNimi"),
        Mapping(source = "erikoisala.id", target = "erikoisalaId"),
        Mapping(source = "erikoisala.nimi", target = "erikoisalaNimi"),
        Mapping(source = "erikoisala.liittynytElsaan", target = "erikoisalaLiittynytElsaan"),
        Mapping(source = "opintoopas.id", target = "opintoopasId"),
        Mapping(source = "opintoopas.nimi", target = "opintoopasNimi"),
    )
    override fun toDto(entity: Opintooikeus): OpintooikeusDTO

    @Mappings(
        Mapping(source = "erikoisalaId", target = "erikoisala"),
        Mapping(target = "tyoskentelyjaksot", ignore = true)
    )
    override fun toEntity(dto: OpintooikeusDTO): Opintooikeus

    @Named("id")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoId(opintooikeus: Opintooikeus): OpintooikeusDTO

    fun fromId(id: Long?) = id?.let {
        val opintooikeus = Opintooikeus()
        opintooikeus.id = id
        opintooikeus
    }
}
