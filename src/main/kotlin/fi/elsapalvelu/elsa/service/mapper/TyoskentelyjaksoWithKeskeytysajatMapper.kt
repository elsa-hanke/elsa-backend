package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import org.mapstruct.*

@Mapper(
    componentModel = "spring",
    uses = [
        TyoskentelypaikkaMapper::class,
        ErikoisalaMapper::class,
        OpintooikeusMapper::class,
        KeskeytysaikaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TyoskentelyjaksoWithKeskeytysajatMapper :
    EntityMapper<TyoskentelyjaksoDTO, Tyoskentelyjakso> {

    @Mappings(
        Mapping(source = "keskeytykset", target = "poissaolot")
    )
    override fun toDto(entity: Tyoskentelyjakso): TyoskentelyjaksoDTO

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
