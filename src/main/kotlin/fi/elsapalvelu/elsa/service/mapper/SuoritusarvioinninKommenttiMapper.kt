package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.SuoritusarvioinninKommentti
import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninKommenttiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        KayttajaMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritusarvioinninKommenttiMapper :
    EntityMapper<SuoritusarvioinninKommenttiDTO, SuoritusarvioinninKommentti> {

    @Mappings(
        Mapping(source = "suoritusarviointi.id", target = "suoritusarviointiId")
    )
    override fun toDto(entity: SuoritusarvioinninKommentti): SuoritusarvioinninKommenttiDTO

    @Mappings(
        Mapping(source = "suoritusarviointiId", target = "suoritusarviointi.id")
    )
    override fun toEntity(dto: SuoritusarvioinninKommenttiDTO): SuoritusarvioinninKommentti

    fun fromId(id: Long?) = id?.let {
        val suoritusarvioinninKommentti = SuoritusarvioinninKommentti()
        suoritusarvioinninKommentti.id = id
        suoritusarvioinninKommentti
    }
}
