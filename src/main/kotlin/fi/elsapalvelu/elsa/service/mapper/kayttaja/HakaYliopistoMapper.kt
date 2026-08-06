package fi.elsapalvelu.elsa.service.mapper.kayttaja

import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.service.dto.kayttaja.HakaYliopistoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
