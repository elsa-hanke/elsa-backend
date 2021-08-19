package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Arviointityokalu
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ArviointityokaluMapper :
    EntityMapper<ArviointityokaluDTO, Arviointityokalu> {

    override fun toDto(entity: Arviointityokalu): ArviointityokaluDTO

    override fun toEntity(dto: ArviointityokaluDTO): Arviointityokalu

    fun fromId(id: Long?) = id?.let {
        val arviointityokalu = Arviointityokalu()
        arviointityokalu.id = id
        arviointityokalu
    }
}
