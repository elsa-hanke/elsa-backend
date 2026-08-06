package fi.elsapalvelu.elsa.service.mapper.suoritteet

import fi.elsapalvelu.elsa.domain.suoritteet.Suorite
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoriteWithErikoisalaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
