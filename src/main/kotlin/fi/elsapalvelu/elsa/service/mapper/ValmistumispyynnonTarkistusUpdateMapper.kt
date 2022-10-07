package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.service.dto.ValmistumispyynnonTarkistusUpdateDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ValmistumispyynnonTarkistusUpdateMapper :
    EntityMapper<ValmistumispyynnonTarkistusUpdateDTO, ValmistumispyynnonTarkistus> {

    override fun toDto(entity: ValmistumispyynnonTarkistus): ValmistumispyynnonTarkistusUpdateDTO

    override fun toEntity(dto: ValmistumispyynnonTarkistusUpdateDTO): ValmistumispyynnonTarkistus
}
