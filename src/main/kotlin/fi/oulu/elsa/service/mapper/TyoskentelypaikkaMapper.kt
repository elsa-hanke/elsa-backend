package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.Tyoskentelypaikka
import fi.oulu.elsa.service.dto.TyoskentelypaikkaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Tyoskentelypaikka] and its DTO [TyoskentelypaikkaDTO].
 */
@Mapper(componentModel = "spring", uses = [TyoskentelyjaksoMapper::class])
interface TyoskentelypaikkaMapper :
    EntityMapper<TyoskentelypaikkaDTO, Tyoskentelypaikka> {

    @Mappings(
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId")
    )
    override fun toDto(tyoskentelypaikka: Tyoskentelypaikka): TyoskentelypaikkaDTO

    @Mappings(
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso")
    )
    override fun toEntity(tyoskentelypaikkaDTO: TyoskentelypaikkaDTO): Tyoskentelypaikka

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val tyoskentelypaikka = Tyoskentelypaikka()
        tyoskentelypaikka.id = id
        tyoskentelypaikka
    }
}
