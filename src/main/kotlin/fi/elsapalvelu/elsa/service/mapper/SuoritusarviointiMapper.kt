package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Suoritusarviointi] and its DTO [SuoritusarviointiDTO].
 */
@Mapper(componentModel = "spring", uses = [TyoskentelyjaksoMapper::class, ErikoistuvaLaakariMapper::class, KayttajaMapper::class, EpaOsaamisalueMapper::class])
interface SuoritusarviointiMapper :
    EntityMapper<SuoritusarviointiDTO, Suoritusarviointi> {

    @Mappings(
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId"),
        Mapping(source = "arvioitava.id", target = "arvioitavaId"),
        Mapping(source = "arvioija.id", target = "arvioijaId"),
        Mapping(source = "arvioitavaOsaalue.id", target = "arvioitavaOsaalueId")
    )
    override fun toDto(suoritusarviointi: Suoritusarviointi): SuoritusarviointiDTO

    @Mappings(
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso"),
        Mapping(target = "osaalueenArviointis", ignore = true),
        Mapping(target = "removeOsaalueenArviointi", ignore = true),
        Mapping(source = "arvioitavaId", target = "arvioitava"),
        Mapping(source = "arvioijaId", target = "arvioija"),
        Mapping(source = "arvioitavaOsaalueId", target = "arvioitavaOsaalue")
    )
    override fun toEntity(suoritusarviointiDTO: SuoritusarviointiDTO): Suoritusarviointi

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val suoritusarviointi = Suoritusarviointi()
        suoritusarviointi.id = id
        suoritusarviointi
    }
}
