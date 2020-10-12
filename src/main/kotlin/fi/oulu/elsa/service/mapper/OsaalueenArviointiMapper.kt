package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.OsaalueenArviointi
import fi.oulu.elsa.service.dto.OsaalueenArviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [OsaalueenArviointi] and its DTO [OsaalueenArviointiDTO].
 */
@Mapper(componentModel = "spring", uses = [ArvioitavaOsaalueMapper::class, SuoritusarviointiMapper::class])
interface OsaalueenArviointiMapper :
    EntityMapper<OsaalueenArviointiDTO, OsaalueenArviointi> {

    @Mappings(
        Mapping(source = "arvioitavaOsaalue.id", target = "arvioitavaOsaalueId"),
        Mapping(source = "suoritusarviointi.id", target = "suoritusarviointiId")
    )
    override fun toDto(osaalueenArviointi: OsaalueenArviointi): OsaalueenArviointiDTO

    @Mappings(
        Mapping(source = "arvioitavaOsaalueId", target = "arvioitavaOsaalue"),
        Mapping(source = "suoritusarviointiId", target = "suoritusarviointi")
    )
    override fun toEntity(osaalueenArviointiDTO: OsaalueenArviointiDTO): OsaalueenArviointi

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val osaalueenArviointi = OsaalueenArviointi()
        osaalueenArviointi.id = id
        osaalueenArviointi
    }
}
