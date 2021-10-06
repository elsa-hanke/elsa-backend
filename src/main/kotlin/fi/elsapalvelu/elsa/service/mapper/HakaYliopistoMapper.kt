package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.service.dto.HakaYliopistoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface HakaYliopistoMapper :
    EntityMapper<HakaYliopistoDTO, Yliopisto> {

    override fun toEntity(dto: HakaYliopistoDTO): Yliopisto

    fun fromId(id: Long?) = id?.let {
        val yliopisto = Yliopisto()
        yliopisto.id = id
        yliopisto
    }
}
