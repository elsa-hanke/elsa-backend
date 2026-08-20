package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymys
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluKysymysDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(componentModel = "spring", uses = [ArviointityokaluKysymysVaihtoehtoMapper::class], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ArviointityokaluKysymysMapper :
    EntityMapper<ArviointityokaluKysymysDTO, ArviointityokaluKysymys> {

    override fun toDto(entity: ArviointityokaluKysymys): ArviointityokaluKysymysDTO

    override fun toEntity(dto: ArviointityokaluKysymysDTO): ArviointityokaluKysymys

    fun fromId(id: Long?) = id?.let {
        val arviointityokaluKysymys = ArviointityokaluKysymys()
        arviointityokaluKysymys.id = id
        arviointityokaluKysymys
    }
}
