package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Suoritemerkinta
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        SuoriteMapper::class,
        TyoskentelyjaksoMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritemerkintaMapper :
    EntityMapper<SuoritemerkintaDTO, Suoritemerkinta> {

    @Mappings(
        Mapping(source = "suorite.id", target = "suoriteId"),
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId")
    )
    override fun toDto(entity: Suoritemerkinta): SuoritemerkintaDTO

    @Mappings(
        Mapping(source = "suoriteId", target = "suorite"),
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso")
    )
    override fun toEntity(dto: SuoritemerkintaDTO): Suoritemerkinta

    fun fromId(id: Long?) = id?.let {
        val suoritemerkinta = Suoritemerkinta()
        suoritemerkinta.id = id
        suoritemerkinta
    }
}
