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
        Mapping(source = "user.avatar", target = "avatar"),
        Mapping(
            target = "nimi",
            expression = "java(entity.getUser() == null ? \"\" : entity.getUser().getFirstName() + \" \" + entity.getUser().getLastName())"
        ),
        Mapping(source = "user.firstName", target = "etunimi"),
        Mapping(source = "user.lastName", target = "sukunimi"),
        Mapping(source = "user.email", target = "sahkoposti"),
        Mapping(source = "user.authorities", target = "authorities"),
        Mapping(source = "yliopistot", target = "yliopistot", qualifiedByName = ["idSet"]),
        Mapping(source = "erikoisalat", target = "erikoisalat", qualifiedByName = ["idSet"]),
    )
    override fun toDto(entity: Kayttaja): KayttajaDTO

    @Mappings(
        Mapping(source = "userId", target = "user"),
        Mapping(source = "etunimi", target = "user.firstName"),
        Mapping(source = "sukunimi", target = "user.lastName"),
        Mapping(source = "sahkoposti", target = "user.email"),
        Mapping(target = "saadutValtuutukset", ignore = true)
    )
    override fun toEntity(dto: KayttajaDTO): Kayttaja

    fun fromId(id: Long?) = id?.let {
        val kayttaja = Kayttaja()
        kayttaja.id = id
        kayttaja
    }
}
