package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArviointiasteikonTaso
import fi.elsapalvelu.elsa.service.dto.ArviointiasteikonTasoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArviointiasteikonTasoMapper :
    EntityMapper<ArviointiasteikonTasoDTO, ArviointiasteikonTaso> {

    override fun toDto(entity: ArviointiasteikonTaso): ArviointiasteikonTasoDTO

    override fun toEntity(dto: ArviointiasteikonTasoDTO): ArviointiasteikonTaso

    fun fromId(id: Long?) = id?.let {
        val arviointiasteikonTaso = ArviointiasteikonTaso()
        arviointiasteikonTaso.id = id
        arviointiasteikonTaso
    }
}
