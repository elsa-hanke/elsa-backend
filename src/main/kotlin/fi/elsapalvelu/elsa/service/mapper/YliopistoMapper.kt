package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
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
