package fi.elsapalvelu.elsa.service.mapper.perustiedot

import fi.elsapalvelu.elsa.domain.perustiedot.Kunta
import fi.elsapalvelu.elsa.service.dto.perustiedot.KuntaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KuntaMapper :
    EntityMapper<KuntaDTO, Kunta> {

    override fun toEntity(dto: KuntaDTO): Kunta

    fun fromId(id: String?) = id?.let {
        val kunta = Kunta()
        kunta.id = id
        kunta
    }
}
