package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.Osoite
import fi.oulu.elsa.service.dto.OsoiteDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Osoite] and its DTO [OsoiteDTO].
 */
@Mapper(componentModel = "spring", uses = [ErikoistuvaLaakariMapper::class])
interface OsoiteMapper :
    EntityMapper<OsoiteDTO, Osoite> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId")
    )
    override fun toDto(osoite: Osoite): OsoiteDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari")
    )
    override fun toEntity(osoiteDTO: OsoiteDTO): Osoite

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val osoite = Osoite()
        osoite.id = id
        osoite
    }
}
