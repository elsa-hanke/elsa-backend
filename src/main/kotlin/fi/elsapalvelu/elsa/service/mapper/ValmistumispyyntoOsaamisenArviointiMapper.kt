package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoOsaamisenArviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        OpintooikeusMapper::class,
        ErikoistuvaLaakariMapper::class,
        KayttajaMapper::class,
        UserMapper::class
    ], unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface ValmistumispyyntoOsaamisenArviointiMapper : EntityMapper<ValmistumispyyntoOsaamisenArviointiDTO, Valmistumispyynto> {
    @Mappings(
        Mapping(source = "opintooikeus.erikoistuvaLaakari.kayttaja.nimi", target = "erikoistujanNimi"),
        Mapping(source = "opintooikeus.erikoistuvaLaakari.kayttaja.user.avatar", target = "erikoistujanAvatar"),
        Mapping(source = "opintooikeus.opiskelijatunnus", target = "erikoistujanOpiskelijatunnus"),
        Mapping(source = "opintooikeus.erikoistuvaLaakari.syntymaaika", target = "erikoistujanSyntymaaika"),
        Mapping(source = "opintooikeus.yliopisto.nimi", target = "erikoistujanYliopisto"),
        Mapping(source = "opintooikeus.erikoisala.nimi", target = "erikoistujanErikoisala"),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaiva",
            target = "erikoistujanLaillistamispaiva"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamistodistus.data",
            target = "erikoistujanLaillistamistodistus"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi",
            target = "erikoistujanLaillistamistodistusNimi"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi",
            target = "erikoistujanLaillistamistodistusTyyppi"
        ),
        Mapping(source = "opintooikeus.asetus.nimi", target = "erikoistujanAsetus"),
        Mapping(source = "opintooikeus.id", target = "opintooikeusId"),
        Mapping(source = "opintooikeus.opintooikeudenMyontamispaiva", target = "opintooikeudenMyontamispaiva"),
        Mapping(target = "vastuuhenkiloOsaamisenArvioijaNimi", source = "vastuuhenkiloOsaamisenArvioija.nimi"),
        Mapping(target = "vastuuhenkiloOsaamisenArvioijaNimike", source = "vastuuhenkiloOsaamisenArvioija.nimike")
    )
    override fun toDto(entity: Valmistumispyynto): ValmistumispyyntoOsaamisenArviointiDTO
}
