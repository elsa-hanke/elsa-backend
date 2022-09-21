package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.service.dto.ValmistumispyynnonTarkistusDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ValmistumispyyntoMapper::class
    ], unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ValmistumispyynnonTarkistusMapper :
    EntityMapper<ValmistumispyynnonTarkistusDTO, ValmistumispyynnonTarkistus> {

    override fun toDto(entity: ValmistumispyynnonTarkistus): ValmistumispyynnonTarkistusDTO

    override fun toEntity(dto: ValmistumispyynnonTarkistusDTO): ValmistumispyynnonTarkistus
}
