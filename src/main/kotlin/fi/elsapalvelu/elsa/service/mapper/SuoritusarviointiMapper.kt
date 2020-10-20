package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Suoritusarviointi] and its DTO [SuoritusarviointiDTO].
 */
@Mapper(componentModel = "spring", uses = [
    ErikoistuvaLaakariMapper::class,
    KayttajaMapper::class,
    EpaOsaamisalueMapper::class,
    TyoskentelyjaksoMapper::class
])
interface SuoritusarviointiMapper :
    EntityMapper<SuoritusarviointiDTO, Suoritusarviointi> {

    @Mappings(
        Mapping(source = "arvioitava.id", target = "arvioitavaId"),
        Mapping(source = "arvioija.id", target = "arvioijaId"),
        Mapping(source = "arvioitavaOsaalue.id", target = "arvioitavaOsaalueId"),
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId")
    )
    override fun toDto(suoritusarviointi: Suoritusarviointi): SuoritusarviointiDTO

    @Mappings(
        Mapping(target = "osaalueenArviointis", ignore = true),
        Mapping(target = "removeOsaalueenArviointi", ignore = true),
        Mapping(source = "arvioitavaId", target = "arvioitava"),
        Mapping(source = "arvioijaId", target = "arvioija"),
        Mapping(source = "arvioitavaOsaalueId", target = "arvioitavaOsaalue"),
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso")
    )
    override fun toEntity(suoritusarviointiDTO: SuoritusarviointiDTO): Suoritusarviointi

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val suoritusarviointi = Suoritusarviointi()
        suoritusarviointi.id = id
        suoritusarviointi
    }
}
