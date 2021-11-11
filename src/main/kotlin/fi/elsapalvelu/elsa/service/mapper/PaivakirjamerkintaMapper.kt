package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Paivakirjamerkinta
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        PaivakirjaAihekategoriaMapper::class,
        ErikoistuvaLaakariMapper::class,
        TeoriakoulutusMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface PaivakirjamerkintaMapper : EntityMapper<PaivakirjamerkintaDTO, Paivakirjamerkinta> {

    @Mappings(
        Mapping(target = "aihekategoriat", source = "aihekategoriat", qualifiedByName = ["idSet"]),
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId"),
    )
    override fun toDto(entity: Paivakirjamerkinta): PaivakirjamerkintaDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari"),
    )
    override fun toEntity(dto: PaivakirjamerkintaDTO): Paivakirjamerkinta
}
