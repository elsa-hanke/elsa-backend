package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ArviointityokaluKysymysVaihtoehto
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKysymysVaihtoehtoDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", uses = [], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ArviointityokaluKysymysVaihtoehtoMapper :
    EntityMapper<ArviointityokaluKysymysVaihtoehtoDTO, ArviointityokaluKysymysVaihtoehto> {

    override fun toDto(entity: ArviointityokaluKysymysVaihtoehto): ArviointityokaluKysymysVaihtoehtoDTO

    override fun toEntity(dto: ArviointityokaluKysymysVaihtoehtoDTO): ArviointityokaluKysymysVaihtoehto

    fun fromId(id: Long?) = id?.let {
        val arviointityokaluKysymysVaihtoehto = ArviointityokaluKysymysVaihtoehto()
        arviointityokaluKysymysVaihtoehto.id = id
        arviointityokaluKysymysVaihtoehto
    }
}
