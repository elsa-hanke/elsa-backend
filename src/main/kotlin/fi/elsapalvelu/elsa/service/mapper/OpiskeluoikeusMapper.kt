package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Opiskeluoikeus
import fi.elsapalvelu.elsa.service.dto.OpiskeluoikeusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoistuvaLaakariMapper::class,
        YliopistoMapper::class,
        ErikoisalaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OpiskeluoikeusMapper :
    EntityMapper<OpiskeluoikeusDTO, Opiskeluoikeus> {

    @Mappings()
    override fun toDto(entity: Opiskeluoikeus): OpiskeluoikeusDTO

    @Mappings()
    override fun toEntity(dto: OpiskeluoikeusDTO): Opiskeluoikeus
}
