package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring", uses = [
        ErikoistuvaLaakariMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TeoriakoulutusMapper :
    EntityMapper<TeoriakoulutusDTO, Teoriakoulutus> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId"),
    )
    override fun toDto(entity: Teoriakoulutus): TeoriakoulutusDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari"),
    )
    override fun toEntity(dto: TeoriakoulutusDTO): Teoriakoulutus
}
