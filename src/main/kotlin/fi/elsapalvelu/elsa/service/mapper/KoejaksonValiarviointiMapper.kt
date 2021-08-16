package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi
import fi.elsapalvelu.elsa.service.dto.KoejaksonValiarviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy


@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface KoejaksonValiarviointiMapper :
    EntityMapper<KoejaksonValiarviointiDTO, KoejaksonValiarviointi> {

    @Mappings(
        Mapping(source = "lahikouluttaja.id", target = "lahikouluttaja.id"),
        Mapping(source = "lahikouluttaja.user.id", target = "lahikouluttaja.kayttajaUserId"),
        Mapping(source = "lahikouluttajanNimi", target = "lahikouluttaja.nimi"),
        Mapping(source = "lahikouluttajaHyvaksynyt", target = "lahikouluttaja.sopimusHyvaksytty"),
        Mapping(source = "lahikouluttajanKuittausaika", target = "lahikouluttaja.kuittausaika"),
        Mapping(source = "lahiesimies.id", target = "lahiesimies.id"),
        Mapping(source = "lahiesimies.user.id", target = "lahiesimies.kayttajaUserId"),
        Mapping(source = "lahiesimiehenNimi", target = "lahiesimies.nimi"),
        Mapping(source = "lahiesimiesHyvaksynyt", target = "lahiesimies.sopimusHyvaksytty"),
        Mapping(source = "lahiesimiehenKuittausaika", target = "lahiesimies.kuittausaika")
    )
    override fun toDto(entity: KoejaksonValiarviointi): KoejaksonValiarviointiDTO

    @Mappings(
        Mapping(source = "lahikouluttaja.id", target = "lahikouluttaja"),
        Mapping(source = "lahikouluttaja.nimi", target = "lahikouluttajanNimi"),
        Mapping(source = "lahikouluttaja.sopimusHyvaksytty", target = "lahikouluttajaHyvaksynyt"),
        Mapping(source = "lahikouluttaja.kuittausaika", target = "lahikouluttajanKuittausaika"),
        Mapping(source = "lahiesimies.id", target = "lahiesimies"),
        Mapping(source = "lahiesimies.nimi", target = "lahiesimiehenNimi"),
        Mapping(source = "lahiesimies.sopimusHyvaksytty", target = "lahiesimiesHyvaksynyt"),
        Mapping(source = "lahiesimies.kuittausaika", target = "lahiesimiehenKuittausaika")
    )
    override fun toEntity(dto: KoejaksonValiarviointiDTO): KoejaksonValiarviointi

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val valiarviointi = KoejaksonValiarviointi()
        valiarviointi.id = id
        valiarviointi
    }
}
