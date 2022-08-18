package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Suorite
import fi.elsapalvelu.elsa.service.dto.SuoriteWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [SuoritteenKategoriaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoriteWithErikoisalaMapper : EntityMapper<SuoriteWithErikoisalaDTO, Suorite> {

    override fun toDto(entity: Suorite): SuoriteWithErikoisalaDTO

    override fun toEntity(dto: SuoriteWithErikoisalaDTO): Suorite

    fun fromId(id: Long?) = id?.let {
        val suorite = Suorite()
        suorite.id = id
        suorite
    }
}
