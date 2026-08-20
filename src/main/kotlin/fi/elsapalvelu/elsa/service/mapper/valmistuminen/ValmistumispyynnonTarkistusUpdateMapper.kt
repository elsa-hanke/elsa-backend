package fi.elsapalvelu.elsa.service.mapper.valmistuminen

import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusUpdateDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ValmistumispyynnonTarkistusUpdateMapper :
    EntityMapper<ValmistumispyynnonTarkistusUpdateDTO, ValmistumispyynnonTarkistus> {

    override fun toDto(entity: ValmistumispyynnonTarkistus): ValmistumispyynnonTarkistusUpdateDTO

    override fun toEntity(dto: ValmistumispyynnonTarkistusUpdateDTO): ValmistumispyynnonTarkistus
}
