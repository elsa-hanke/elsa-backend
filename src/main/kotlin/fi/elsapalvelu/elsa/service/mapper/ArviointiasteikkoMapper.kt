package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Arviointiasteikko
import fi.elsapalvelu.elsa.service.dto.ArviointiasteikkoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

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
