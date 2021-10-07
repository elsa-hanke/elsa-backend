package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        TyoskentelypaikkaMapper::class,
        ErikoisalaMapper::class,
        ErikoistuvaLaakariMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TyoskentelyjaksoMapper :
    EntityMapper<TyoskentelyjaksoDTO, Tyoskentelyjakso> {

    @Mappings(
        Mapping(source = "omaaErikoisalaaTukeva.id", target = "omaaErikoisalaaTukevaId"),
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId"),
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
        Mapping(source = "omaaErikoisalaaTukevaId", target = "omaaErikoisalaaTukeva"),
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari")
    )
    override fun toEntity(dto: TyoskentelyjaksoDTO): Tyoskentelyjakso

    fun fromId(id: Long?) = id?.let {
        val tyoskentelyjakso = Tyoskentelyjakso()
        tyoskentelyjakso.id = id
        tyoskentelyjakso
    }
}
