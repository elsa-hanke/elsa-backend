package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.OsaamisalueenArviointi
import fi.elsapalvelu.elsa.service.dto.OsaamisalueenArviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [OsaamisalueenArviointi] and its DTO [OsaamisalueenArviointiDTO].
 */
@Mapper(componentModel = "spring", uses = [OsaamisenArviointiMapper::class])
interface OsaamisalueenArviointiMapper :
    EntityMapper<OsaamisalueenArviointiDTO, OsaamisalueenArviointi> {

    @Mappings(
        Mapping(source = "osaamisenArviointi.id", target = "osaamisenArviointiId")
    )
    override fun toDto(osaamisalueenArviointi: OsaamisalueenArviointi): OsaamisalueenArviointiDTO

    @Mappings(
        Mapping(target = "arviointiosaalues", ignore = true),
        Mapping(target = "removeArviointiosaalue", ignore = true),
        Mapping(source = "osaamisenArviointiId", target = "osaamisenArviointi")
    )
    override fun toEntity(osaamisalueenArviointiDTO: OsaamisalueenArviointiDTO): OsaamisalueenArviointi

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val osaamisalueenArviointi = OsaamisalueenArviointi()
        osaamisalueenArviointi.id = id
        osaamisalueenArviointi
    }
}
