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
        OppimistavoiteMapper::class,
        TyoskentelyjaksoMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritemerkintaMapper :
    EntityMapper<SuoritemerkintaDTO, Suoritemerkinta> {

    @Mappings(
        Mapping(source = "oppimistavoite.id", target = "oppimistavoiteId"),
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId"),
        Mapping(
            source = "oppimistavoite.kategoria.erikoisala.arviointiasteikko",
            target = "arviointiasteikko"
        )
    )
    override fun toDto(entity: Suoritemerkinta): SuoritemerkintaDTO

    @Mappings(
        Mapping(source = "oppimistavoiteId", target = "oppimistavoite"),
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso")
    )
    override fun toEntity(dto: SuoritemerkintaDTO): Suoritemerkinta

    fun fromId(id: Long?) = id?.let {
        val suoritemerkinta = Suoritemerkinta()
        suoritemerkinta.id = id
        suoritemerkinta
    }
}
