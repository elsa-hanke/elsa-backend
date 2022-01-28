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
        TeoriakoulutusMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface PaivakirjamerkintaMapper : EntityMapper<PaivakirjamerkintaDTO, Paivakirjamerkinta> {

    @Mappings(
        Mapping(target = "aihekategoriat", source = "aihekategoriat", qualifiedByName = ["idSet"])
    )
    override fun toDto(entity: Paivakirjamerkinta): PaivakirjamerkintaDTO

    override fun toEntity(dto: PaivakirjamerkintaDTO): Paivakirjamerkinta
}
