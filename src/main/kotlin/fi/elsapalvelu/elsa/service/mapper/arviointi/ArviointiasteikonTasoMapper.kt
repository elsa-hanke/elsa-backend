package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArviointiasteikonTaso
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointiasteikonTasoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
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
