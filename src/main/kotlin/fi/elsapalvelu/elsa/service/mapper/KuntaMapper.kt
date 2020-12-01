package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Kunta
import fi.elsapalvelu.elsa.service.dto.KuntaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KuntaMapper :
    EntityMapper<KuntaDTO, Kunta> {

    override fun toEntity(dto: KuntaDTO): Kunta

    @JvmDefault
    fun fromId(id: String?) = id?.let {
        val kunta = Kunta()
        kunta.id = id
        kunta
    }
}
