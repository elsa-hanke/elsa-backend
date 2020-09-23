package fi.oulu.elsa.service.mapper

import fi.oulu.elsa.domain.Yliopisto
import fi.oulu.elsa.service.dto.YliopistoDTO
import org.mapstruct.Mapper

/**
 * Mapper for the entity [Yliopisto] and its DTO [YliopistoDTO].
 */
@Mapper(componentModel = "spring", uses = [ErikoisalaMapper::class])
interface YliopistoMapper :
    EntityMapper<YliopistoDTO, Yliopisto> {

    override fun toEntity(yliopistoDTO: YliopistoDTO): Yliopisto

    @JvmDefault
    fun fromId(id: Long?) = id?.let {
        val yliopisto = Yliopisto()
        yliopisto.id = id
        yliopisto
    }
}
