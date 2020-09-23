package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.ErikoistuvaLaakari
import fi.oulu.elsa.service.dto.ErikoistuvaLaakariDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [ErikoistuvaLaakari] and its DTO [ErikoistuvaLaakariDTO].
 */
@Mapper(componentModel = "spring", uses = [KayttajaMapper::class])
interface ErikoistuvaLaakariMapper :
    EntityMapper<ErikoistuvaLaakariDTO, ErikoistuvaLaakari> {

    @Mappings(
        Mapping(source = "kayttaja.id", target = "kayttajaId")
    )
    override fun toDto(erikoistuvaLaakari: ErikoistuvaLaakari): ErikoistuvaLaakariDTO

    @Mappings(
        Mapping(source = "kayttajaId", target = "kayttaja"),
        Mapping(target = "osoites", ignore = true),
        Mapping(target = "removeOsoite", ignore = true),
        Mapping(target = "tyoskentelyjaksos", ignore = true),
        Mapping(target = "removeTyoskentelyjakso", ignore = true),
        Mapping(target = "hops", ignore = true),
        Mapping(target = "koejakso", ignore = true)
    )
    override fun toEntity(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakari

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val erikoistuvaLaakari = ErikoistuvaLaakari()
        erikoistuvaLaakari.id = id
        erikoistuvaLaakari
    }
}
