package fi.elsapalvelu.elsa.service.mapper.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelypaikka
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelypaikkaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.KuntaMapper
@Mapper(
    componentModel = "spring",
    uses = [
        KuntaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TyoskentelypaikkaMapper :
    EntityMapper<TyoskentelypaikkaDTO, Tyoskentelypaikka> {

    @Mappings(
        Mapping(source = "kunta.id", target = "kuntaId")
    )
    override fun toDto(entity: Tyoskentelypaikka): TyoskentelypaikkaDTO

    @Mappings(
        Mapping(source = "kuntaId", target = "kunta"),
        Mapping(target = "tyoskentelyjakso", ignore = true)
    )
    override fun toEntity(dto: TyoskentelypaikkaDTO): Tyoskentelypaikka

    fun fromId(id: Long?) = id?.let {
        val tyoskentelypaikka = Tyoskentelypaikka()
        tyoskentelypaikka.id = id
        tyoskentelypaikka
    }
}
