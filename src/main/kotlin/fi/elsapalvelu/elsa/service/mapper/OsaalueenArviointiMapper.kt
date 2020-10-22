package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.OsaalueenArviointi
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [ArvioitavaOsaalueMapper::class, SuoritusarviointiMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OsaalueenArviointiMapper :
    EntityMapper<OsaalueenArviointiDTO, OsaalueenArviointi> {

    @Mappings(
        Mapping(source = "arvioitavaOsaalue.id", target = "arvioitavaOsaalueId"),
        Mapping(source = "suoritusarviointi.id", target = "suoritusarviointiId")
    )
    override fun toDto(entity: OsaalueenArviointi): OsaalueenArviointiDTO

    @Mappings(
        Mapping(source = "arvioitavaOsaalueId", target = "arvioitavaOsaalue"),
        Mapping(source = "suoritusarviointiId", target = "suoritusarviointi")
    )
    override fun toEntity(dto: OsaalueenArviointiDTO): OsaalueenArviointi

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val osaalueenArviointi = OsaalueenArviointi()
        osaalueenArviointi.id = id
        osaalueenArviointi
    }
}
