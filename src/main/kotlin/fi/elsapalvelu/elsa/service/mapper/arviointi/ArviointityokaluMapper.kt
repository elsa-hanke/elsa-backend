package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(componentModel = "spring", uses = [ArviointityokaluKysymysMapper::class], unmappedTargetPolicy = ReportingPolicy.IGNORE)
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
