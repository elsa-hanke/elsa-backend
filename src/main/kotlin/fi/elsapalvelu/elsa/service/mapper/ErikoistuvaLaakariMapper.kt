package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        KayttajaMapper::class,
        ErikoisalaMapper::class,
        OpiskeluoikeusMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ErikoistuvaLaakariMapper :
    EntityMapper<ErikoistuvaLaakariDTO, ErikoistuvaLaakari> {

    @Mappings(
        Mapping(source = "kayttaja.nimi", target = "nimi"),
        Mapping(source = "kayttaja.id", target = "kayttajaId"),
        Mapping(source = "kayttaja.user.email", target = "sahkoposti"),
        Mapping(source = "kayttaja.user.phoneNumber", target = "puhelinnumero"),
        Mapping(source = "yliopistoNimi", target = "yliopisto")
    )
    override fun toDto(entity: ErikoistuvaLaakari): ErikoistuvaLaakariDTO

    @Mappings(
        Mapping(source = "kayttajaId", target = "kayttaja"),
        Mapping(target = "tyoskentelyjaksot", ignore = true),
    )
    override fun toEntity(dto: ErikoistuvaLaakariDTO): ErikoistuvaLaakari

    fun fromId(id: Long?) = id?.let {
        val erikoistuvaLaakari = ErikoistuvaLaakari()
        erikoistuvaLaakari.id = id
        erikoistuvaLaakari
    }
}
