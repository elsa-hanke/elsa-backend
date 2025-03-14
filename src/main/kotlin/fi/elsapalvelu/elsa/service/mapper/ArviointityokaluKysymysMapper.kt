package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymys
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKysymysDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

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
