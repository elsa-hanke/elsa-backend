package fi.elsapalvelu.elsa.service.mapper.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

import fi.elsapalvelu.elsa.service.mapper.EntityMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.perustiedot.YliopistoMapper
@Mapper(
    componentModel = "spring",
    uses = [
        UserMapper::class,
        YliopistoMapper::class,
        ErikoisalaMapper::class,
        KayttajaYliopistoErikoisalaMapper::class
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
        Mapping(source = "user.phoneNumber", target = "puhelin"),
        Mapping(source = "user.eppn", target = "eppn"),
        Mapping(source = "user.authorities", target = "authorities")
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
