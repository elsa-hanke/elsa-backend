package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    uses = [
        ErikoistuvaLaakariMapper::class,
        KayttajaMapper::class,
        ArvioitavaKokonaisuusMapper::class,
        TyoskentelyjaksoMapper::class,
        SuoritusarvioinninKommenttiMapper::class,
        ArviointityokaluMapper::class,
        AsiakirjaMapper::class,
        ArviointiasteikkoMapper::class,
        SuoritusarvioinninArvioitavaKokonaisuusMapper::class,
        SuoritusarvioinninArviointityokalunVastausMapper::class
    ],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SuoritusarviointiMapper :
    EntityMapper<SuoritusarviointiDTO, Suoritusarviointi> {

    @Mappings(
        Mapping(source = "arvioinninAntaja.id", target = "arvioinninAntajaId"),
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId"),
        Mapping(
            source = "tyoskentelyjakso.opintooikeus.erikoistuvaLaakari.kayttaja",
            target = "arvioinninSaaja"
        )
    )
    override fun toDto(entity: Suoritusarviointi): SuoritusarviointiDTO

    @Mappings(
        Mapping(target = "kommentit", ignore = true),
        Mapping(source = "arvioinninAntajaId", target = "arvioinninAntaja"),
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso")
    )
    override fun toEntity(dto: SuoritusarviointiDTO): Suoritusarviointi

    fun fromId(id: Long?) = id?.let {
        val suoritusarviointi = Suoritusarviointi()
        suoritusarviointi.id = id
        suoritusarviointi
    }
}
