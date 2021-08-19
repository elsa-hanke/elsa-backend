package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Asiakirja
import fi.elsapalvelu.elsa.service.dto.AsiakirjaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        TyoskentelyjaksoMapper::class,
        ErikoistuvaLaakariMapper::class,
        AsiakirjaDataMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface AsiakirjaMapper : EntityMapper<AsiakirjaDTO, Asiakirja> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId"),
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId"),
    )
    override fun toDto(entity: Asiakirja): AsiakirjaDTO

    @Mappings(
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso"),
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari")
    )
    override fun toEntity(dto: AsiakirjaDTO): Asiakirja

    fun fromId(id: Long?) = id?.let {
        val asiakirja = Asiakirja()
        asiakirja.id = id
        asiakirja
    }
}
