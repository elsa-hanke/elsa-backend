package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.service.RichTextHtmlSanitizer
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget

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

    @AfterMapping
    fun sanitizeDto(@MappingTarget dto: ArviointityokaluDTO) {
        dto.ohjeteksti = RichTextHtmlSanitizer.sanitize(dto.ohjeteksti)
    }

    @AfterMapping
    fun sanitizeEntity(source: ArviointityokaluDTO, @MappingTarget entity: Arviointityokalu) {
        entity.ohjeteksti = RichTextHtmlSanitizer.sanitize(source.ohjeteksti ?: entity.ohjeteksti)
    }
}
