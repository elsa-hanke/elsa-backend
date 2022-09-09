package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.TerveyskeskuskoulutusjaksonHyvaksynta
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TerveyskeskuskoulutusjaksonHyvaksyntaMapper :
    EntityMapper<TerveyskeskuskoulutusjaksonHyvaksyntaDTO, TerveyskeskuskoulutusjaksonHyvaksynta> {

    @Mappings(
        Mapping(source = "opintooikeus.erikoisala.nimi", target = "erikoistuvanErikoisala"),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.nimi",
            target = "erikoistuvanNimi"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.user.avatar",
            target = "erikoistuvanAvatar"
        ),
        Mapping(source = "opintooikeus.opiskelijatunnus", target = "erikoistuvanOpiskelijatunnus"),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.syntymaaika",
            target = "erikoistuvanSyntymaaika"
        ),
        Mapping(source = "opintooikeus.yliopisto.nimi", target = "erikoistuvanYliopisto"),
        Mapping(source = "opintooikeus.asetus.nimi", target = "asetus"),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaiva",
            target = "laillistamispaiva"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedosto",
            target = "laillistamispaivanLiite"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi",
            target = "laillistamispaivanLiitteenNimi"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi",
            target = "laillistamispaivanLiitteenTyyppi"
        ),
        Mapping(
            source = "virkailija.nimi",
            target = "virkailijanNimi"
        ),
        Mapping(
            source = "virkailija.nimike",
            target = "virkailijanNimike"
        )
    )
    override fun toDto(entity: TerveyskeskuskoulutusjaksonHyvaksynta): TerveyskeskuskoulutusjaksonHyvaksyntaDTO

    override fun toEntity(dto: TerveyskeskuskoulutusjaksonHyvaksyntaDTO): TerveyskeskuskoulutusjaksonHyvaksynta
}
