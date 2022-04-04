package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy


@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class, KoulutussopimuksenKouluttajaMapper::class, KoulutussopimuksenKoulutuspaikkaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoejaksonKoulutussopimusMapper :
    EntityMapper<KoejaksonKoulutussopimusDTO, KoejaksonKoulutussopimus> {

    @Mappings(
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.user.avatar",
            target = "erikoistuvanAvatar"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.nimi",
            target = "erikoistuvanNimi"
        ),
        Mapping(source = "opintooikeus.opiskelijatunnus", target = "erikoistuvanOpiskelijatunnus"),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.syntymaaika",
            target = "erikoistuvanSyntymaaika"
        ),
        Mapping(source = "opintooikeus.yliopisto.nimi", target = "erikoistuvanYliopisto"),
        Mapping(source = "opintooikeus.erikoisala.nimi", target = "erikoistuvanErikoisala"),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.user.email",
            target = "erikoistuvanSahkoposti"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.user.phoneNumber",
            target = "erikoistuvanPuhelinnumero"
        ),
        Mapping(
            source = "opintooikeus.opintooikeudenMyontamispaiva",
            target = "opintooikeudenMyontamispaiva"
        ),
        Mapping(source = "vastuuhenkiloHyvaksynyt", target = "vastuuhenkilo.sopimusHyvaksytty"),
        Mapping(source = "vastuuhenkilonKuittausaika", target = "vastuuhenkilo.kuittausaika"),
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo.id"),
        Mapping(source = "vastuuhenkilo.nimike", target = "vastuuhenkilo.nimike"),
        Mapping(
            source = "vastuuhenkilo.user.email",
            target = "vastuuhenkilo.sahkoposti"
        ),
        Mapping(
            source = "vastuuhenkilo.user.phoneNumber",
            target = "vastuuhenkilo.puhelin"
        )
    )
    override fun toDto(entity: KoejaksonKoulutussopimus): KoejaksonKoulutussopimusDTO

    @Mappings(
        Mapping(source = "vastuuhenkilo.sopimusHyvaksytty", target = "vastuuhenkiloHyvaksynyt"),
        Mapping(source = "vastuuhenkilo.kuittausaika", target = "vastuuhenkilonKuittausaika"),
        Mapping(source = "vastuuhenkilo.id", target = "vastuuhenkilo")
    )
    override fun toEntity(dto: KoejaksonKoulutussopimusDTO): KoejaksonKoulutussopimus

    fun fromId(id: Long?) = id?.let {
        val koulutussopimus = KoejaksonKoulutussopimus()
        koulutussopimus.id = id
        koulutussopimus
    }
}
