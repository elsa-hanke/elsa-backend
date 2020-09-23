package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.Hops
import fi.oulu.elsa.service.dto.HopsDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Hops] and its DTO [HopsDTO].
 */
@Mapper(componentModel = "spring", uses = [ErikoistuvaLaakariMapper::class])
interface HopsMapper :
    EntityMapper<HopsDTO, Hops> {

    @Mappings(
        Mapping(source = "erikoistuvaLaakari.id", target = "erikoistuvaLaakariId")
    )
    override fun toDto(hops: Hops): HopsDTO

    @Mappings(
        Mapping(source = "erikoistuvaLaakariId", target = "erikoistuvaLaakari")
    )
    override fun toEntity(hopsDTO: HopsDTO): Hops

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val hops = Hops()
        hops.id = id
        hops
    }
}
