package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy


@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoejaksonVastuuhenkilonArvioMapper :
    EntityMapper<KoejaksonVastuuhenkilonArvioDTO, KoejaksonVastuuhenkilonArvio> {

    @Mappings(
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.user.avatar",
            target = "erikoistuvanAvatar"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.nimi",
            target = "erikoistuvanNimi"
        ),
        Mapping(source = "opintooikeus.yliopisto.nimi", target = "erikoistuvanYliopisto"),
        Mapping(source = "opintooikeus.erikoisala.nimi", target = "erikoistuvanErikoisala"),
        Mapping(source = "opintooikeus.opiskelijatunnus", target = "erikoistuvanOpiskelijatunnus"),
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo.id"),
        Mapping(
            source = "vastuuhenkiloHyvaksynyt",
            target = "vastuuhenkilo.sopimusHyvaksytty"
        ),
        Mapping(source = "vastuuhenkilonKuittausaika", target = "vastuuhenkilo.kuittausaika"),
        Mapping(
            source = "virkailijaHyvaksynyt",
            target = "virkailija.sopimusHyvaksytty"
        ),
        Mapping(source = "virkailijanKuittausaika", target = "virkailija.kuittausaika")
    )
    override fun toDto(entity: KoejaksonVastuuhenkilonArvio): KoejaksonVastuuhenkilonArvioDTO

    @Mappings(
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo"),
        Mapping(source = "vastuuhenkilo.sopimusHyvaksytty", target = "vastuuhenkiloHyvaksynyt"),
        Mapping(source = "vastuuhenkilo.kuittausaika", target = "vastuuhenkilonKuittausaika"),
        Mapping(source = "virkailija.sopimusHyvaksytty", target = "virkailijaHyvaksynyt"),
        Mapping(source = "virkailija.kuittausaika", target = "virkailijanKuittausaika")
    )
    override fun toEntity(dto: KoejaksonVastuuhenkilonArvioDTO): KoejaksonVastuuhenkilonArvio

    fun fromId(id: Long?) = id?.let {
        val loppukeskustelu = KoejaksonLoppukeskustelu()
        loppukeskustelu.id = id
        loppukeskustelu
    }
}
