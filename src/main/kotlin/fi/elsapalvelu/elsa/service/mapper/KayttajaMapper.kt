package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        YliopistoMapper::class,
        AuthorityMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KayttajaMapper :
    EntityMapper<KayttajaDTO, Kayttaja> {

    @Mappings()
    override fun toDto(entity: Kayttaja): KayttajaDTO

    @Mappings(
        Mapping(target = "saadutValtuutukset", ignore = true),
        Mapping(target = "removeValtuutus", ignore = true)
    )
    override fun toEntity(dto: KayttajaDTO): Kayttaja

    fun fromId(id: String?) = id?.let {
        val kayttaja = Kayttaja()
        kayttaja.id = id
        kayttaja
    }
}
