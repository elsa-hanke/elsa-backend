package fi.elsapalvelu.elsa.service.mapper.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.SuoritusarvioinninArviointityokalunVastaus
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarvioinninArviointityokalunVastausDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
@Mapper(
    componentModel = "spring",
    uses = [
        SuoritusarviointiMapper::class,
        ArviointityokaluMapper::class,
        ArviointityokaluKysymysMapper::class,
        ArviointityokaluKysymysVaihtoehtoMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritusarvioinninArviointityokalunVastausMapper :
    EntityMapper<SuoritusarvioinninArviointityokalunVastausDTO, SuoritusarvioinninArviointityokalunVastaus> {

    @Mappings(
        Mapping(source = "suoritusarviointi.id", target = "suoritusarviointiId"),
        Mapping(source = "arviointityokalu.id", target = "arviointityokaluId"),
        Mapping(source = "arviointityokaluKysymys.id", target = "arviointityokaluKysymysId"),
        Mapping(source = "valittuVaihtoehto.id", target = "valittuVaihtoehtoId")
    )
    override fun toDto(entity: SuoritusarvioinninArviointityokalunVastaus): SuoritusarvioinninArviointityokalunVastausDTO

    @Mappings(
        Mapping(source = "suoritusarviointiId", target = "suoritusarviointi.id"),
        Mapping(source = "arviointityokaluId", target = "arviointityokalu.id"),
        Mapping(source = "arviointityokaluKysymysId", target = "arviointityokaluKysymys.id"),
        Mapping(source = "valittuVaihtoehtoId", target = "valittuVaihtoehto.id")
    )
    override fun toEntity(dto: SuoritusarvioinninArviointityokalunVastausDTO): SuoritusarvioinninArviointityokalunVastaus

    fun fromId(id: Long?) = id?.let {
        val vastaus = SuoritusarvioinninArviointityokalunVastaus()
        vastaus.id = id
        vastaus
    }
}
