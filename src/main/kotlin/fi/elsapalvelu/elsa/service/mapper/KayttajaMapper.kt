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
        UserMapper::class,
        YliopistoMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KayttajaMapper :
    EntityMapper<KayttajaDTO, Kayttaja> {

    @Mappings(
        Mapping(source = "user.id", target = "userId"),
        Mapping(
            target = "nimi",
            expression = "java(entity.getUser() == null ? \"\" : entity.getUser().getFirstName() + \" \" + entity.getUser().getLastName())"
        ),
        Mapping(source = "user.authorities", target = "authorities")
    )
    override fun toDto(entity: Kayttaja): KayttajaDTO

    @Mappings(
        Mapping(source = "userId", target = "user"),
        Mapping(target = "saadutValtuutukset", ignore = true),
        Mapping(target = "removeValtuutus", ignore = true)
    )
    override fun toEntity(dto: KayttajaDTO): Kayttaja

    fun fromId(id: Long?) = id?.let {
        val kayttaja = Kayttaja()
        kayttaja.id = id
        kayttaja
    }
}
