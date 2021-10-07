package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.OsaalueenArviointi
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [SuoritusarviointiMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface OsaalueenArviointiMapper :
    EntityMapper<OsaalueenArviointiDTO, OsaalueenArviointi> {

    @Mappings(
        Mapping(source = "suoritusarviointi.id", target = "suoritusarviointiId")
    )
    override fun toDto(entity: OsaalueenArviointi): OsaalueenArviointiDTO

    @Mappings(
        Mapping(source = "suoritusarviointiId", target = "suoritusarviointi")
    )
    override fun toEntity(dto: OsaalueenArviointiDTO): OsaalueenArviointi

    fun fromId(id: Long?) = id?.let {
        val osaalueenArviointi = OsaalueenArviointi()
        osaalueenArviointi.id = id
        osaalueenArviointi
    }
}
