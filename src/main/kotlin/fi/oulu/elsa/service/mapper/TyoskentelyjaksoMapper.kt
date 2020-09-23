package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.Tyoskentelyjakso
import fi.oulu.elsa.service.dto.TyoskentelyjaksoDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Tyoskentelyjakso] and its DTO [TyoskentelyjaksoDTO].
 */
@Mapper(componentModel = "spring", uses = [ErikoistuvaLaakariMapper::class])
interface TyoskentelyjaksoMapper :
    EntityMapper<TyoskentelyjaksoDTO, Tyoskentelyjakso> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId")
    )
    override fun toDto(tyoskentelyjakso: Tyoskentelyjakso): TyoskentelyjaksoDTO

    @Mappings(
        Mapping(target = "tyoskentelypaikka", ignore = true),
        Mapping(target = "arviointi", ignore = true),
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari")
    )
    override fun toEntity(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): Tyoskentelyjakso

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val tyoskentelyjakso = Tyoskentelyjakso()
        tyoskentelyjakso.id = id
        tyoskentelyjakso
    }
}
