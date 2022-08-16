package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.service.dto.SeurantajaksoDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy


@Mapper(
    componentModel = "spring",
    uses = [KayttajaMapper::class, KoulutusjaksoMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface SeurantajaksoMapper :
    EntityMapper<SeurantajaksoDTO, Seurantajakso> {

    @Mappings(
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.kayttaja.nimi",
            target = "erikoistuvanNimi"
        ),
        Mapping(source = "opintooikeus.erikoisala.nimi", target = "erikoistuvanErikoisalaNimi"),
        Mapping(source = "opintooikeus.opiskelijatunnus", target = "erikoistuvanOpiskelijatunnus"),
        Mapping(source = "opintooikeus.yliopisto.nimi", target = "erikoistuvanYliopistoNimi"),
        Mapping(source = "opintooikeus.id", target = "opintooikeusId")
    )
    override fun toDto(entity: Seurantajakso): SeurantajaksoDTO

    override fun toEntity(dto: SeurantajaksoDTO): Seurantajakso

    fun fromId(id: Long?) = id?.let {
        val seurantajakso = Seurantajakso()
        seurantajakso.id = id
        seurantajakso
    }
}
