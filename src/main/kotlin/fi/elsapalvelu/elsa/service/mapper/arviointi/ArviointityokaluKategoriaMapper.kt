package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKategoria
import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointityokaluKategoriaDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(componentModel = "spring", uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ArviointityokaluKategoriaMapper :
    EntityMapper<ArviointityokaluKategoriaDTO, ArviointityokaluKategoria> {

    override fun toDto(entity: ArviointityokaluKategoria): ArviointityokaluKategoriaDTO

    override fun toEntity(dto: ArviointityokaluKategoriaDTO): ArviointityokaluKategoria

    fun fromId(id: Long?) = id?.let {
        val arviointityokaluKategoria = ArviointityokaluKategoria()
        arviointityokaluKategoria.id = id
        arviointityokaluKategoria
    }
}
