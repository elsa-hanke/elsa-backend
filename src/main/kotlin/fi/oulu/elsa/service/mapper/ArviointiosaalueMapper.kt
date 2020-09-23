package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.Arviointiosaalue
import fi.oulu.elsa.service.dto.ArviointiosaalueDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Arviointiosaalue] and its DTO [ArviointiosaalueDTO].
 */
@Mapper(componentModel = "spring", uses = [OsaamisalueenArviointiMapper::class])
interface ArviointiosaalueMapper :
    EntityMapper<ArviointiosaalueDTO, Arviointiosaalue> {

    @Mappings(
        Mapping(source = "osaamisalueenArviointi.id", target = "osaamisalueenArviointiId")
    )
    override fun toDto(arviointiosaalue: Arviointiosaalue): ArviointiosaalueDTO

    @Mappings(
        Mapping(source = "osaamisalueenArviointiId", target = "osaamisalueenArviointi")
    )
    override fun toEntity(arviointiosaalueDTO: ArviointiosaalueDTO): Arviointiosaalue

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val arviointiosaalue = Arviointiosaalue()
        arviointiosaalue.id = id
        arviointiosaalue
    }
}
