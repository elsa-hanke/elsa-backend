package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.OsaamisenArviointi
import fi.oulu.elsa.service.dto.OsaamisenArviointiDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [OsaamisenArviointi] and its DTO [OsaamisenArviointiDTO].
 */
@Mapper(
    componentModel = "spring",
    uses = [
        TyoskentelyjaksoMapper::class,
        ErikoistuvaLaakariMapper::class,
        KayttajaMapper::class
    ]
)
interface OsaamisenArviointiMapper :
    EntityMapper<OsaamisenArviointiDTO, OsaamisenArviointi> {

    @Mappings(
        Mapping(source = "tyoskentelyjakso.id", target = "tyoskentelyjaksoId"),
        Mapping(source = "arvioitava.id", target = "arvioitavaId"),
        Mapping(source = "arvioija.id", target = "arvioijaId")
    )
    override fun toDto(osaamisenArviointi: OsaamisenArviointi): OsaamisenArviointiDTO

    @Mappings(
        Mapping(source = "tyoskentelyjaksoId", target = "tyoskentelyjakso"),
        Mapping(target = "osaamisalueenArviointis", ignore = true),
        Mapping(target = "removeOsaamisalueenArviointi", ignore = true),
        Mapping(source = "arvioitavaId", target = "arvioitava"),
        Mapping(source = "arvioijaId", target = "arvioija")
    )
    override fun toEntity(osaamisenArviointiDTO: OsaamisenArviointiDTO): OsaamisenArviointi

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val osaamisenArviointi = OsaamisenArviointi()
        osaamisenArviointi.id = id
        osaamisenArviointi
    }
}
