package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.Opintooikeustiedot
import fi.oulu.elsa.service.dto.OpintooikeustiedotDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

/**
 * Mapper for the entity [Opintooikeustiedot] and its DTO [OpintooikeustiedotDTO].
 */
@Mapper(componentModel = "spring", uses = [YliopistoMapper::class])
interface OpintooikeustiedotMapper :
    EntityMapper<OpintooikeustiedotDTO, Opintooikeustiedot> {

    @Mappings(
        Mapping(source = "yliopisto.id", target = "yliopistoId")
    )
    override fun toDto(opintooikeustiedot: Opintooikeustiedot): OpintooikeustiedotDTO

    @Mappings(
        Mapping(source = "yliopistoId", target = "yliopisto")
    )
    override fun toEntity(opintooikeustiedotDTO: OpintooikeustiedotDTO): Opintooikeustiedot

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val opintooikeustiedot = Opintooikeustiedot()
        opintooikeustiedot.id = id
        opintooikeustiedot
    }
}
