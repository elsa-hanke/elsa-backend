package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", uses = [ErikoisalaMapper::class], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface YliopistoMapper :
    EntityMapper<YliopistoDTO, Yliopisto> {

    override fun toEntity(dto: YliopistoDTO): Yliopisto

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val yliopisto = Yliopisto()
        yliopisto.id = id
        yliopisto
    }
}
