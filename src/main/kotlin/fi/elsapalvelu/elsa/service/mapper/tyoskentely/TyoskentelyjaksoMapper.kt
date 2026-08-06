package fi.elsapalvelu.elsa.service.mapper.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import org.mapstruct.*

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.kayttaja.OpintooikeusMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
@Mapper(
    componentModel = "spring",
    uses = [
        TyoskentelypaikkaMapper::class,
        ErikoisalaMapper::class,
        OpintooikeusMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TyoskentelyjaksoMapper :
    EntityMapper<TyoskentelyjaksoDTO, Tyoskentelyjakso> {

    @Mappings(
        Mapping(source = "omaaErikoisalaaTukeva.id", target = "omaaErikoisalaaTukevaId"),
        Mapping(
            expression = "java(entity.hasTapahtumia())",
            target = "tapahtumia"
        )
    )
    override fun toDto(entity: Tyoskentelyjakso): TyoskentelyjaksoDTO

    @Mappings(
        Mapping(target = "suoritusarvioinnit", ignore = true),
        Mapping(target = "suoritemerkinnat", ignore = true),
        Mapping(target = "keskeytykset", ignore = true),
        Mapping(source = "omaaErikoisalaaTukevaId", target = "omaaErikoisalaaTukeva")
    )
    override fun toEntity(dto: TyoskentelyjaksoDTO): Tyoskentelyjakso

    fun fromId(id: Long?) = id?.let {
        val tyoskentelyjakso = Tyoskentelyjakso()
        tyoskentelyjakso.id = id
        tyoskentelyjakso
    }

    @Named("idSet")
    @BeanMapping(ignoreByDefault = true)
    @Mappings(
        Mapping(target = "id", source = "id")
    )
    fun toDtoIdSet(tyoskentelyjakso: Set<Tyoskentelyjakso>): Set<TyoskentelyjaksoDTO>
}
