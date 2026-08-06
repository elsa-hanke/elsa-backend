package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.Arviointiasteikko
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointiasteikkoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [
        ArviointiasteikonTasoMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ArviointiasteikkoMapper :
    EntityMapper<ArviointiasteikkoDTO, Arviointiasteikko> {

    override fun toDto(entity: Arviointiasteikko): ArviointiasteikkoDTO

    override fun toEntity(dto: ArviointiasteikkoDTO): Arviointiasteikko

    fun fromId(id: Long?) = id?.let {
        val arviointiasteikko = Arviointiasteikko()
        arviointiasteikko.id = id
        arviointiasteikko
    }
}
